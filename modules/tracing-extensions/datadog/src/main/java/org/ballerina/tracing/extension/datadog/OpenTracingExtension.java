/*
*  Copyright (c) 2017, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
*  WSO2 Inc. licenses this file to you under the Apache License,
*  Version 2.0 (the "License"); you may not use this file except
*  in compliance with the License.
*  You may obtain a copy of the License at
*
*    http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing,
* software distributed under the License is distributed on an
* "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
* KIND, either express or implied.  See the License for the
* specific language governing permissions and limitations
* under the License.
*
*/
package org.ballerina.tracing.extension.datadog;

import datadog.opentracing.DDSpan;
import datadog.opentracing.DDTracer;
import datadog.trace.common.DDTraceConfig;
import datadog.trace.common.util.Clock;
import io.opentracing.Span;
import io.opentracing.Tracer;
import org.ballerina.tracing.core.OpenTracer;
import org.ballerina.tracing.core.SpanFinishRequest;
import org.ballerina.tracing.core.config.InvalidConfigurationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.Queue;

import static org.ballerina.tracing.extension.datadog.Constants.AGENT_HOST;
import static org.ballerina.tracing.extension.datadog.Constants.AGENT_PORT;
import static org.ballerina.tracing.extension.datadog.Constants.DEFAULT_AGENT_HOST;
import static org.ballerina.tracing.extension.datadog.Constants.DEFAULT_AGENT_PORT;
import static org.ballerina.tracing.extension.datadog.Constants.DEFAULT_PRIORITY_SAMPLING;
import static org.ballerina.tracing.extension.datadog.Constants.DEFAULT_SERVICE_NAME;
import static org.ballerina.tracing.extension.datadog.Constants.DEFAULT_WRITER_TYPE;
import static org.ballerina.tracing.extension.datadog.Constants.PRIORITY_SAMPLING;
import static org.ballerina.tracing.extension.datadog.Constants.SERVICE_NAME;
import static org.ballerina.tracing.extension.datadog.Constants.TRACER_NAME;
import static org.ballerina.tracing.extension.datadog.Constants.WRITER_TYPE;


/**
 * This is the open tracing extension class for {@link OpenTracer}
 */
public class OpenTracingExtension implements OpenTracer {
    private Map<Long, SpanFinishRequest> spanHolder = new HashMap<>();

    private static final Logger logger = LoggerFactory.getLogger(OpenTracingExtension.class);

    @Override
    public Tracer getTracer(String tracerName, Properties configProperties) throws InvalidConfigurationException {
        if (!tracerName.equalsIgnoreCase(Constants.TRACER_NAME)) {
            throw new InvalidConfigurationException("Unexpected tracer name! " +
                    "The tracer name supported by this extension is : " + Constants.TRACER_NAME + " but found : "
                    + tracerName);
        }
        validateConfiguration(configProperties);

        DDTraceConfig ddTraceConfig = new DDTraceConfig();
        ddTraceConfig.setProperty(SERVICE_NAME, (String) configProperties.get(SERVICE_NAME));
        ddTraceConfig.setProperty(WRITER_TYPE, (String) configProperties.get(WRITER_TYPE));
        ddTraceConfig.setProperty(AGENT_HOST, (String) configProperties.get(AGENT_HOST));
        ddTraceConfig.setProperty(AGENT_PORT, String.valueOf(configProperties.get(AGENT_PORT)));
        ddTraceConfig.setProperty(PRIORITY_SAMPLING,
                Boolean.toString((Boolean) configProperties.get(PRIORITY_SAMPLING)));
        return new DDTracer(ddTraceConfig);
    }

    public boolean handleFinish(SpanFinishRequest spanFinishRequest) {
        if (spanFinishRequest != null && spanFinishRequest.getSpan() instanceof DDSpan) {
            DDSpan ddSpan = (DDSpan) spanFinishRequest.getSpan();
            if (spanFinishRequest.getFinishTime() == 0L) {
                spanFinishRequest.setFinishTime(Clock.currentMicroTime());
            }
            if (ddSpan.isRootSpan()) {
                Queue spans = ddSpan.context().getTrace();
                Iterator iterator = spans.iterator();
                boolean canFinish = true;
                while (iterator.hasNext()) {
                    DDSpan childSpan = (DDSpan) iterator.next();
                    if (ddSpan != childSpan && childSpan.getDurationNano() == 0L) {
                        this.spanHolder.putIfAbsent(ddSpan.getSpanId(), spanFinishRequest);
                        canFinish = false;
                        break;
                    }
                }
                if (canFinish) {
                    ddSpan.finish(spanFinishRequest.getFinishTime());
                }
            } else {
                ddSpan.finish(spanFinishRequest.getFinishTime());
                handleFinish(this.spanHolder.remove(getRootParentId(ddSpan)));
            }
            return true;
        } else {
            return false;
        }
    }

    @Override
    public Span getSpanWithTraceId(long traceId, Span span) {
        if (span instanceof DDSpan) {
            return DDSpanWrapper.getSpan(traceId, (DDSpan) span);
        }
        return span;
    }

    private Long getRootParentId(DDSpan span) {
        return span.context().getTrace().peek().getSpanId();
    }

    private void validateConfiguration(Properties configuration) {
        setValidatedStringConfig(configuration, SERVICE_NAME, DEFAULT_SERVICE_NAME);
        setValidatedStringConfig(configuration, WRITER_TYPE, DEFAULT_WRITER_TYPE);
        setValidatedStringConfig(configuration, AGENT_HOST, DEFAULT_AGENT_HOST);
        setValidatedIntegerConfig(configuration, AGENT_PORT, DEFAULT_AGENT_PORT);
        setValidatedBooleanConfig(configuration, PRIORITY_SAMPLING, DEFAULT_PRIORITY_SAMPLING);
    }

    private void setValidatedStringConfig(Properties configuration, String configName, String defaultValue) {
        Object configValue = configuration.get(configName);
        if (configValue == null || configValue.toString().trim().isEmpty()) {
            configuration.put(configName, defaultValue);
        } else {
            configuration.put(configName, configValue.toString().trim());
        }
    }

    private void setValidatedIntegerConfig(Properties configuration, String configName, int defaultValue) {
        Object configValue = configuration.get(configName);
        if (configValue == null) {
            configuration.put(configName, defaultValue);
        } else {
            try {
                configuration.put(configName, Integer.parseInt(configValue.toString()));
            } catch (NumberFormatException ex) {
                logger.warn("Open tracing configuration for tracer name - " + TRACER_NAME +
                        " expects configuration element : " + configName + "with integer type but found non integer : "
                        + configValue.toString() + " ! Therefore assigning default value : " + defaultValue
                        + " for " + configName + " configuration.");
                configuration.put(configName, defaultValue);
            }
        }
    }

    private void setValidatedBooleanConfig(Properties configuration, String configName, boolean defaultValue) {
        Object configValue = configuration.get(configName);
        if (configValue == null) {
            configuration.put(configName, defaultValue);
        } else {
            String configStringValue = configValue.toString().trim();
            if (configStringValue.equalsIgnoreCase(Boolean.TRUE.toString())
                    || configStringValue.equalsIgnoreCase(Boolean.FALSE.toString())) {
                configuration.put(configName, Boolean.parseBoolean(configStringValue));
            } else {
                logger.warn("Open tracing configuration for tracer name - " + TRACER_NAME +
                        " expects configuration element : " + configName + "with boolean type (true/false) but " +
                        "found non boolean : " + configStringValue + " ! Therefore assigning default value : "
                        + defaultValue + " for " + configName + " configuration.");
                configuration.put(configName, defaultValue);
            }
        }
    }
}
