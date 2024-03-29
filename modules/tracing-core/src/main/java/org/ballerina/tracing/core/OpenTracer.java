/*
*  Copyright (c) 2018, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
package org.ballerina.tracing.core;

import io.opentracing.Span;
import io.opentracing.Tracer;
import org.ballerina.tracing.core.config.InvalidConfigurationException;

import java.util.Properties;

/**
 * This is the interface that OpenTracerFactory will be using to obtain the {@link Tracer} implementation.
 */
public interface OpenTracer {

    /**
     * Returns the specific tracer implementation of the analytics engine based
     * on the configuration provided.
     *
     * @param tracerName       name of the tracer
     * @param configProperties The configuration of the tracer
     * @return Specific {@link Tracer} instance throws {@link InvalidConfigurationException}
     * if the configuration or tracer name is invalid.
     */
    Tracer getTracer(String tracerName, Properties configProperties) throws InvalidConfigurationException;

    /**
     * This method can handle the customized approach for finishing span by it self, and in that case the
     * return value will be true, therefore the {@link OpenTracerFactory} will not call the span.finish() by it self.
     * In case if the spefic tracers doesn't have any custom implementation required, then it can simply return false,
     * and hence {@link OpenTracerFactory} will handle finishing the span.
     *
     * @param spanFinishRequest The details of the span which supposed to be finished.
     * @return boolean value which represents whether the tracer impl will handle the finishSpan operation.
     */
    boolean handleFinish(SpanFinishRequest spanFinishRequest);


    Span getSpanWithTraceId (long traceId, Span span);
}
