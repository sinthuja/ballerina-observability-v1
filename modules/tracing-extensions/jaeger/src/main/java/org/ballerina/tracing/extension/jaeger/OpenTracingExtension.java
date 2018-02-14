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
package org.ballerina.tracing.extension.jaeger;

import io.opentracing.Tracer;
import org.ballerina.tracing.core.OpenTracer;
import org.ballerina.tracing.core.SpanFinishRequest;
import org.ballerina.tracing.core.config.InvalidConfigurationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;

import static org.ballerina.tracing.extension.jaeger.Constants.DEFAULT_REPORTER_FLUSH_INTERVAL;
import static org.ballerina.tracing.extension.jaeger.Constants.DEFAULT_REPORTER_HOSTNAME;
import static org.ballerina.tracing.extension.jaeger.Constants.DEFAULT_REPORTER_LOG_SPANS;
import static org.ballerina.tracing.extension.jaeger.Constants.DEFAULT_REPORTER_MAX_BUFFER_SPANS;
import static org.ballerina.tracing.extension.jaeger.Constants.DEFAULT_REPORTER_PORT;
import static org.ballerina.tracing.extension.jaeger.Constants.DEFAULT_SAMPLER_PARAM;
import static org.ballerina.tracing.extension.jaeger.Constants.DEFAULT_SAMPLER_TYPE;
import static org.ballerina.tracing.extension.jaeger.Constants.REPORTER_FLUSH_INTERVAL_MS_CONFIG;
import static org.ballerina.tracing.extension.jaeger.Constants.REPORTER_HOST_NAME_CONFIG;
import static org.ballerina.tracing.extension.jaeger.Constants.REPORTER_LOG_SPANS_CONFIG;
import static org.ballerina.tracing.extension.jaeger.Constants.REPORTER_MAX_BUFFER_SPANS_CONFIG;
import static org.ballerina.tracing.extension.jaeger.Constants.REPORTER_PORT_CONFIG;
import static org.ballerina.tracing.extension.jaeger.Constants.SAMPLER_PARAM_CONFIG;
import static org.ballerina.tracing.extension.jaeger.Constants.SAMPLER_TYPE_CONFIG;
import static org.ballerina.tracing.extension.jaeger.Constants.TRACER_NAME;


/**
 * This is the open tracing extension class for {@link OpenTracer}
 */
public class OpenTracingExtension implements OpenTracer {

    private static final Logger logger = LoggerFactory.getLogger(OpenTracingExtension.class);

    @Override
    public Tracer getTracer(String tracerName, Properties configProperties) throws InvalidConfigurationException {
        if (!tracerName.equalsIgnoreCase(TRACER_NAME)) {
            throw new InvalidConfigurationException("Unexpected tracer name! " +
                    "The tracer name supported by this extension is : " + TRACER_NAME + " but found : "
                    + tracerName);
        }
        validateConfiguration(configProperties);
        return new com.uber.jaeger.Configuration(
                org.ballerina.tracing.core.Constants.OPEN_TRACING_COMPONENT_NAME,
                new com.uber.jaeger.Configuration.SamplerConfiguration(
                        (String) configProperties.get(SAMPLER_TYPE_CONFIG)
                        , (Integer) configProperties.get(SAMPLER_PARAM_CONFIG)),
                new com.uber.jaeger.Configuration.ReporterConfiguration(
                        (Boolean) configProperties.get(REPORTER_LOG_SPANS_CONFIG),
                        (String) configProperties.get(REPORTER_HOST_NAME_CONFIG),
                        (Integer) configProperties.get(REPORTER_PORT_CONFIG),
                        (Integer) configProperties.get(REPORTER_FLUSH_INTERVAL_MS_CONFIG),
                        (Integer) configProperties.get(REPORTER_MAX_BUFFER_SPANS_CONFIG))
        ).getTracerBuilder().build();
    }

    @Override
    public boolean handleFinish(SpanFinishRequest spanFinishRequest) {
        return false;
    }

    private void validateConfiguration(Properties configuration) {
        setValidatedStringConfig(configuration, SAMPLER_TYPE_CONFIG, DEFAULT_SAMPLER_TYPE);
        setValidatedIntegerConfig(configuration, SAMPLER_PARAM_CONFIG, DEFAULT_SAMPLER_PARAM.intValue());
        setValidatedBooleanConfig(configuration, REPORTER_LOG_SPANS_CONFIG, DEFAULT_REPORTER_LOG_SPANS);
        setValidatedStringConfig(configuration, REPORTER_HOST_NAME_CONFIG, DEFAULT_REPORTER_HOSTNAME);
        setValidatedIntegerConfig(configuration, REPORTER_PORT_CONFIG, DEFAULT_REPORTER_PORT);
        setValidatedIntegerConfig(configuration, REPORTER_FLUSH_INTERVAL_MS_CONFIG, DEFAULT_REPORTER_FLUSH_INTERVAL);
        setValidatedIntegerConfig(configuration, REPORTER_MAX_BUFFER_SPANS_CONFIG, DEFAULT_REPORTER_MAX_BUFFER_SPANS);
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
