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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * This is the in memory span registry which keep track of the spans that are started.
 */
public class SpanHolder {

    private static final SpanHolder spanHolder = new SpanHolder();
    private Map<String, Map<String, List<Span>>> spanCache;

    private SpanHolder() {
        spanCache = new HashMap<>();
    }

    public static SpanHolder getInstance() {
        return spanHolder;
    }

    public String onBuildSpan(String invocationId, List<Span> span) {
        String spanId = UUID.randomUUID().toString();
        Map<String, List<Span>> spans = spanCache.get(invocationId);
        if (spans == null) {
            synchronized (this) {
                spans = spanCache.get(invocationId);
                if (spans == null) {
                    spans = new HashMap<>();
                    spanCache.put(invocationId, spans);
                }
            }
        }
        spans.put(spanId, span);
        return spanId;
    }

    public List<Span> onFinishSpan(String invocationId, String spanId) {
        Map<String, List<Span>> spans = spanCache.get(invocationId);
        List<Span> span = null;
        if (spans != null) {
            span = spans.remove(spanId);
            if (spans.isEmpty()) {
                spanCache.remove(invocationId);
            }
        }
        return span;
    }
}
