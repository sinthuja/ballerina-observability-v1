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
import io.opentracing.SpanContext;
import io.opentracing.Tracer;
import io.opentracing.propagation.Format;
import io.opentracing.propagation.TextMap;
import org.ballerina.tracing.core.config.ConfigLoader;
import org.ballerina.tracing.core.config.InvalidConfigurationException;
import org.ballerina.tracing.core.config.OpenTracingConfig;
import org.ballerina.tracing.core.config.TracerConfig;
import org.ballerina.tracing.core.config.TracingDepth;
import org.ballerina.tracing.core.exception.UnknownSpanContextTypeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * This is the class which holds the tracers that are enabled, and bridges all tracers with instrumented code.
 */
public class OpenTracerFactory {

    private static final Logger logger = LoggerFactory.getLogger(OpenTracerFactory.class);
    private static OpenTracerFactory instance = new OpenTracerFactory();
    private OpenTracingConfig openTracingConfig;
    private Map<String, Tracer> tracers;

    private OpenTracerFactory() {
        try {
            this.openTracingConfig = ConfigLoader.load();
            if (this.openTracingConfig != null) {
                this.tracers = new HashMap<>();
                loadTracers();
            }
        } catch (IllegalAccessException
                | InstantiationException | ClassNotFoundException | InvalidConfigurationException ex) {
            logger.error("Error while loading the open tracing configuration, " +
                    "failed to initialize the tracer instance", ex);
        }
    }


    public static OpenTracerFactory getInstance() {
        return instance;
    }

    public TracingDepth getTracingDepth() {
        boolean tracersExists = this.openTracingConfig != null && !this.tracers.isEmpty();
        if (tracersExists) {
            return this.openTracingConfig.getDepth();
        } else {
            return TracingDepth.NONE;
        }
    }

    private TracerConfig getTracingConfig(String tracerName) {
        return this.openTracingConfig.getTracer(tracerName);
    }

    private void register(String tracerName, Tracer tracer) {
        TracerConfig tracerConfig = getTracingConfig(tracerName);
        if (tracerConfig.isEnabled() && this.tracers.get(tracerName.toLowerCase(Locale.ENGLISH)) == null) {
            this.tracers.put(tracerName.toLowerCase(Locale.ENGLISH), tracer);
        }
    }

    private void loadTracers() throws ClassNotFoundException, IllegalAccessException, InstantiationException
            , InvalidConfigurationException {
        for (TracerConfig tracerConfig : this.openTracingConfig.getTracers()) {
            if (tracerConfig.isEnabled()) {
                Class<?> openTracerClass = Class.forName(tracerConfig.getClassName()).asSubclass(OpenTracer.class);
                OpenTracer openTracer = (OpenTracer) openTracerClass.newInstance();
                Tracer tracer = openTracer.getTracer(tracerConfig.getName(),
                        tracerConfig.getConfiguration());
                register(tracerConfig.getName(), tracer);
            }
        }
    }

    public Map<String, Object> extract(Format<TextMap> format, TextMap carrier) {
        Map<String, Object> spanContext = new HashMap<>();
        if (format == null) {
            format = Format.Builtin.HTTP_HEADERS;
        }
        for (Map.Entry<String, Tracer> tracerEntry : this.tracers.entrySet()) {
            spanContext.put(tracerEntry.getKey(), tracerEntry.getValue().extract(format, carrier));
        }
        return spanContext;
    }

    public List<Span> buildSpan(String spanName, Map<String, Object> spanContextMap, Map<String, String> tags,
                                boolean makeActive) {
        List<Span> spanList = new ArrayList<>();
        for (Map.Entry spanContextEntry : spanContextMap.entrySet()) {
            Tracer tracer = this.tracers.get(spanContextEntry.getKey().toString());
            Tracer.SpanBuilder spanBuilder = tracer.buildSpan(spanName);
            for (Map.Entry<String, String> tag : tags.entrySet()) {
                spanBuilder = spanBuilder.withTag(tag.getKey(), tag.getValue());
            }
            if (spanContextEntry.getValue() != null) {
                if (spanContextEntry.getValue() instanceof SpanContext) {
                    spanBuilder = spanBuilder.asChildOf((SpanContext) spanContextEntry.getValue());
                } else if (spanContextEntry.getValue() instanceof Span) {
                    spanBuilder = spanBuilder.asChildOf((Span) spanContextEntry.getValue());
                } else {
                    throw new UnknownSpanContextTypeException("Unknown span context field - " +
                            spanContextEntry.getValue().getClass()
                            + "! Open tracing can span can be build only by using "
                            + SpanContext.class + " or " + Span.class);
                }
            }
            Span span = spanBuilder.start();
            if (makeActive) {
                tracer.scopeManager().activate(span, false);
            }
            spanList.add(span);
        }
        return spanList;
    }


    private void finishSpan(List<Span> spanList) {
        spanList.forEach(Span::finish);
    }

    public Map<String, Object> getActiveSpans() {
        Map<String, Object> activeSpanMap = new HashMap<>();
        boolean isActiveExists = false;
        for (Map.Entry<String, Tracer> tracerEntry : this.tracers.entrySet()) {
            Span activeSpan = tracerEntry.getValue().activeSpan();
            if (activeSpan != null) {
                isActiveExists = true;
            }
            activeSpanMap.put(tracerEntry.getKey(), tracerEntry.getValue().activeSpan());
        }
        if (isActiveExists) {
            return activeSpanMap;
        } else {
            return null;
        }
    }


    public void inject(Map<String, Span> activeSpanMap, Format<TextMap> format, TextMap carrier) {
        for (Map.Entry<String, Span> activeSpanEntry : activeSpanMap.entrySet()) {
            Tracer tracer = this.tracers.get(activeSpanEntry.getKey());
            if (tracer != null) {
                tracer.inject(activeSpanEntry.getValue().context(), format, carrier);
            }
        }
    }

    public void finishSpan(List<Span> span, Map<String, Object> parent) {
        finishSpan(span);
        if (parent != null) {
            for (Map.Entry<String, Object> parentSpan : parent.entrySet()) {
                if (parentSpan.getValue() != null) {
                    if (parentSpan.getValue() instanceof Span) {
                    this.tracers.get(parentSpan.getKey().toLowerCase(Locale.ENGLISH)).scopeManager().
                            activate((Span) parentSpan.getValue(), false);
                    } else {
                        throw new UnknownSpanContextTypeException("Only " + Span.class
                                + " as parent span can be captured " +
                                "and activated! But found " + parentSpan.getClass());
                    }
                }
            }
        }
    }

}
