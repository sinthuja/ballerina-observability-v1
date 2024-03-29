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
package org.ballerina.tracing;

import io.opentracing.Span;
import org.ballerina.tracing.core.*;
import org.ballerina.tracing.core.Constants;
import org.ballerinalang.bre.Context;
import org.ballerinalang.model.types.TypeKind;
import org.ballerinalang.model.values.BMap;
import org.ballerinalang.model.values.BString;
import org.ballerinalang.model.values.BStruct;
import org.ballerinalang.model.values.BValue;
import org.ballerinalang.natives.AbstractNativeFunction;
import org.ballerinalang.natives.annotations.BallerinaFunction;
import org.ballerinalang.natives.annotations.ReturnType;
import org.ballerinalang.net.http.HttpUtil;
import org.wso2.transport.http.netty.message.HTTPCarbonMessage;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * This is function which implements the buildSpan method for tracing.
 */
@BallerinaFunction(
        packageName = "ballerina.tracing",
        functionName = "buildSpan",
        returnType = {@ReturnType(type = TypeKind.STRING)},
        isPublic = true
)

public class BuildSpan extends AbstractNativeFunction {
    @Override
    public BValue[] execute(Context context) {
        BStruct httpRequest = (BStruct) getRefArgument(context, 0);
        BMap tags = (BMap) getRefArgument(context, 1);
        String spanName = getStringArgument(context, 0);
        boolean makeActive = getBooleanArgument(context, 0);
        long invocationId;
        if (context.getProperties().get(Constants.INVOCATION_ID_PROPERTY) != null) {
            invocationId = (Long) context.getProperties().get(Constants.INVOCATION_ID_PROPERTY);
        } else {
            invocationId = ThreadLocalRandom.current().nextLong(0, Long.MAX_VALUE);
            context.setProperty(Constants.INVOCATION_ID_PROPERTY, invocationId);
        }

        boolean hasParent = true;
        OpenTracerFactory.ActiveSpanResponse response = OpenTracerFactory.getInstance().getActiveSpans();
        Map<String, Object> spanContext;
        if (response == null) {
            hasParent = false;
            spanContext = (Map<String, Object>) context.getProperty(Utils.getPropertyNameForParentSpanHolder());
            if (spanContext == null) {
                HTTPCarbonMessage carbonMessage = HttpUtil.getCarbonMsg(httpRequest, null);
                spanContext = OpenTracerFactory.getInstance().extract(null,
                        new RequestExtractor(carbonMessage.getHeaders().iteratorAsString()));
            }
        } else {
            spanContext = response.getSpans();
            if (!response.isHasAllSpans()) {
                Map<String, Object> workerSpans = (Map<String, Object>)
                        context.getProperty(Utils.getPropertyNameForParentSpanHolder());
                for (String tracerKey : response.getTracerKeys()) {
                    spanContext.put(tracerKey, workerSpans.get(tracerKey));
                }
            }
        }


        List<Span> spanList = OpenTracerFactory.getInstance().buildSpan(invocationId, spanName, spanContext,
                Utils.toStringMap(tags),
                makeActive);
        //TODO: get id from the invocationContext, and pass it.
        if (hasParent) {
            return getBValues(new BString(SpanHolder.getInstance().onBuildSpan("xxxxxxx", spanList, spanContext)));
        } else {
            return getBValues(new BString(SpanHolder.getInstance().onBuildSpan("xxxxxxx", spanList, null)));
        }
    }
}
