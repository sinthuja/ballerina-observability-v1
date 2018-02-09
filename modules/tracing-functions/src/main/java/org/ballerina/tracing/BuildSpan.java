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
import org.ballerina.tracing.core.OpenTracerFactory;
import org.ballerina.tracing.core.RequestExtractor;
import org.ballerina.tracing.core.SpanHolder;
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
import org.ballerinalang.util.tracer.TracerRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.transport.http.netty.message.HTTPCarbonMessage;

import java.util.List;
import java.util.Map;

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
    private static final Logger logger = LoggerFactory.getLogger(OpenTracerFactory.class);


    @Override
    public BValue[] execute(Context context) {
        BStruct httpRequest = (BStruct) getRefArgument(context, 0);
        BMap tags = (BMap) getRefArgument(context, 1);
        String spanName = getStringArgument(context, 0);
        boolean makeActive = getBooleanArgument(context, 0);

        boolean hasParent = true;
        Map<String, Object> spanContext = OpenTracerFactory.getInstance().getActiveSpans();
        if (spanContext == null) {
            hasParent = false;
            Map<String, Object> scopeMap = (Map<String, Object>) context.getProperty(Utils.getPropertyNameForParentSpanHolder());
            if (scopeMap != null) {
                OpenTracerFactory.getInstance().setScopes(scopeMap);
                spanContext = OpenTracerFactory.getInstance().getActiveSpans();
            } else {
                HTTPCarbonMessage carbonMessage = HttpUtil.getCarbonMsg(httpRequest, null);
                spanContext = OpenTracerFactory.getInstance().extract(null,
                        new RequestExtractor(carbonMessage.getHeaders().iteratorAsString()));
            }
        }

        List<Span> spanList = OpenTracerFactory.getInstance().buildSpan(spanName, spanContext,
                Utils.toStringMap(tags),
                makeActive);
        //TODO: get id from the invocationContext, and pass it.
        System.out.println("Function - > " + TracerRegistry.getInstance().getTracer());
        System.out.print("Build span - " + spanName + " , Thread ID - > " + Thread.currentThread().getId());
        if (hasParent) {
            return getBValues(new BString(SpanHolder.getInstance().onBuildSpan("xxxxxxx", spanList, spanContext)));
        } else {
            return getBValues(new BString(SpanHolder.getInstance().onBuildSpan("xxxxxxx", spanList, null)));
        }
    }
}
