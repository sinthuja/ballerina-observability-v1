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
package org.ballerina.tracing.extension.zipkin;

import brave.Tracing;
import brave.opentracing.BraveTracer;
import io.opentracing.Tracer;
import org.ballerina.tracing.core.OpenTracer;
import org.ballerina.tracing.core.config.InvalidConfigurationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import zipkin2.reporter.AsyncReporter;
import zipkin2.reporter.Sender;
import zipkin2.reporter.okhttp3.OkHttpSender;

import java.util.Properties;

import static org.ballerina.tracing.extension.zipkin.Constants.DEFAULT_REPORTER_HOSTNAME;
import static org.ballerina.tracing.extension.zipkin.Constants.DEFAULT_REPORTER_PORT;
import static org.ballerina.tracing.extension.zipkin.Constants.REPORTER_HOST_NAME_CONFIG;
import static org.ballerina.tracing.extension.zipkin.Constants.REPORTER_PORT_CONFIG;
import static org.ballerina.tracing.extension.zipkin.Constants.TRACER_NAME;


/**
 * This is the open tracing extension class for {@link OpenTracer}
 */
public class OpenTracingExtension implements OpenTracer {

    private static final Logger logger = LoggerFactory.getLogger(OpenTracingExtension.class);

    @Override
    public Tracer getTracer(String tracerName, Properties configProperties) throws InvalidConfigurationException {
        if (!tracerName.equalsIgnoreCase(Constants.TRACER_NAME)) {
            throw new InvalidConfigurationException("Unexpected tracer name! " +
                    "The tracer name supported by this extension is : " + Constants.TRACER_NAME + " but found : "
                    + tracerName);
        }
        validateConfiguration(configProperties);
        Sender sender = OkHttpSender.create(
                "http://" +
                        configProperties.get(REPORTER_HOST_NAME_CONFIG) + ":" +
                        configProperties.get(REPORTER_PORT_CONFIG) +
                        Constants.REPORTING_API_CONTEXT);
        AsyncReporter reporter = AsyncReporter.create(sender);
        return BraveTracer.create(Tracing.newBuilder()
                .localServiceName(org.ballerina.tracing.core.Constants.OPEN_TRACING_COMPONENT_NAME)
                .spanReporter(reporter)
                .build());
    }

    @Override
    public boolean supportParallelExec() {
        return true;
    }

    private void validateConfiguration(Properties configuration) {
        setValidatedStringConfig(configuration, REPORTER_HOST_NAME_CONFIG, DEFAULT_REPORTER_HOSTNAME);
        setValidatedIntegerConfig(configuration, REPORTER_PORT_CONFIG, DEFAULT_REPORTER_PORT);
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
}
