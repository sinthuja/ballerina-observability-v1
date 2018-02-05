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

package org.ballerina.platform.tracing.core;

import io.opentracing.ActiveSpan;
import io.opentracing.BaseSpan;
import io.opentracing.Span;
import io.opentracing.SpanContext;
import io.opentracing.Tracer;
import io.opentracing.propagation.Format;
import io.opentracing.propagation.TextMap;
import org.ballerina.platform.tracing.core.config.ConfigLoader;
import org.ballerina.platform.tracing.core.config.InvalidConfigurationException;
import org.ballerina.platform.tracing.core.config.OpenTracingConfig;
import org.ballerina.platform.tracing.core.config.TracerConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

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

    public boolean isTracingEnabled() {
        return this.openTracingConfig != null && !this.tracers.isEmpty();
    }

    public TracerConfig getTracingConfig(String tracerName) {
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
                } else if (spanContextEntry.getValue() instanceof BaseSpan) {
                    spanBuilder = spanBuilder.asChildOf((BaseSpan) spanContextEntry.getValue());
                } else {
                    throw new UnknownSpanContextTypeException("Unknown span context field - " +
                            spanContextEntry.getValue().getClass()
                            + "! Open tracing can span can be build only by using "
                            + SpanContext.class + " or " + BaseSpan.class);
                }
            }
            Span span = spanBuilder.startManual();
            if (makeActive) {
                tracer.makeActive(span);
            }
            spanList.add(span);
        }
        return spanList;
    }


    public void finishSpan(List<Span> spanList) {
        spanList.forEach(Span::finish);
    }

    public Map<String, Object> getActiveSpans() {
        Map<String, Object> activeSpanMap = new HashMap<>();
        for (Map.Entry<String, Tracer> tracerEntry : this.tracers.entrySet()) {
            activeSpanMap.put(tracerEntry.getKey(), tracerEntry.getValue().activeSpan());
        }
        return activeSpanMap;
    }

    public Map<String, ActiveSpan> getActiveSpans(Set<String> tracerNames) {
        Map<String, ActiveSpan> activeSpanMap = new HashMap<>();
        for (String tracerName : tracerNames) {
            activeSpanMap.put(tracerName.toLowerCase(Locale.ENGLISH),
                    this.tracers.get(tracerName.toLowerCase(Locale.ENGLISH)).activeSpan());
        }
        return activeSpanMap;
    }


    public void inject(Map<String, ActiveSpan> activeSpanMap, Format<TextMap> format, TextMap carrier) {
        for (Map.Entry<String, ActiveSpan> activeSpanEntry : activeSpanMap.entrySet()) {
            Tracer tracer = this.tracers.get(activeSpanEntry.getKey());
            if (tracer != null) {
                tracer.inject(activeSpanEntry.getValue().context(), format, carrier);
            }
        }
    }

    public void finishSpan(List<Span> span, Map<String, Object> parent) {
        finishSpan(span);
        for (Map.Entry<String, Object> parentSpan : parent.entrySet()) {
            if (parentSpan.getValue() != null) {
                if (parentSpan.getValue() instanceof ActiveSpan) {
                    ((ActiveSpan) parentSpan.getValue()).capture().activate();
                } else {
                    throw new UnknownSpanContextTypeException("Only " + ActiveSpan.class
                            + " as parent span can be captured " +
                            "and activated! But found " + parentSpan.getClass());
                }
            }
        }
    }

}
