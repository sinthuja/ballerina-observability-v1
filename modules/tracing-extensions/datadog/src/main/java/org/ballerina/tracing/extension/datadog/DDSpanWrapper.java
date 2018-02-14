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
package org.ballerina.tracing.extension.datadog;

import datadog.opentracing.DDSpan;
import datadog.opentracing.DDSpanContext;

import java.util.HashMap;
import java.util.Map;


/**
 * Wrapper for the Span to use the custom trace ID.
 */
public class DDSpanWrapper extends DDSpan {

    private DDSpanWrapper(long timestampMicro, DDSpanContext context) {
        super(timestampMicro, context);
    }

    public static DDSpan getSpan(long traceId, DDSpan span) {
        DDSpanContext originalContext = span.context();
        originalContext.getTrace().remove(); // To remove the reference of the previous span that was created.
        DDSpanContext context = new DDSpanContext(traceId, originalContext.getSpanId(),
                originalContext.getParentId(), originalContext.getServiceName(), originalContext.getOperationName(),
                originalContext.getResourceName(), originalContext.getSamplingPriority(),
                originalContext.getBaggageItems(),
                originalContext.getErrorFlag(), originalContext.getSpanType(), getNewTags(originalContext.getTags()),
                originalContext.getTrace(), originalContext.getTracer());
        return new DDSpanWrapper(span.getStartTime() / 1000L, context);
    }

    private static Map<String, Object> getNewTags(Map<String, Object> originalTags) {
        Map<String, Object> tags = new HashMap<>();
        for (Map.Entry<String, Object> tag : originalTags.entrySet()) {
            tags.put(tag.getKey(), tag.getValue());
        }
        return tags;
    }

}
