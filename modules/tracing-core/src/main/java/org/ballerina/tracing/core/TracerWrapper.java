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

import io.opentracing.ScopeManager;
import io.opentracing.Span;
import io.opentracing.SpanContext;
import io.opentracing.Tracer;
import io.opentracing.propagation.Format;

/**
 * This is the tracer wrapper to handle the parallism in for the tracers which doesn't support it.
 */
public class TracerWrapper implements Tracer {
    private Tracer tracer;
    private boolean parallelExec;

    public TracerWrapper(Tracer tracer, boolean supportParallelExec) {
        this.tracer = tracer;
        this.parallelExec = supportParallelExec;
    }

    @Override
    public ScopeManager scopeManager() {
        return this.tracer.scopeManager();
    }

    @Override
    public Span activeSpan() {
        return this.tracer.activeSpan();
    }

    @Override
    public SpanBuilder buildSpan(String s) {
        return this.tracer.buildSpan(s);
    }

    @Override
    public <C> void inject(SpanContext spanContext, Format<C> format, C c) {
        this.tracer.inject(spanContext, format, c);
    }

    @Override
    public <C> SpanContext extract(Format<C> format, C c) {
        return this.tracer.extract(format, c);
    }

    public boolean isParallelExec() {
        return parallelExec;
    }
}
