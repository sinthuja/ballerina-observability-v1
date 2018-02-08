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
import org.ballerina.tracing.core.SpanHolder;
import org.ballerinalang.bre.Context;
import org.ballerinalang.model.types.TypeKind;
import org.ballerinalang.model.values.BBoolean;
import org.ballerinalang.model.values.BValue;
import org.ballerinalang.natives.AbstractNativeFunction;
import org.ballerinalang.natives.annotations.BallerinaFunction;
import org.ballerinalang.natives.annotations.ReturnType;

import java.util.List;

/**
 * This is function which implements the finish span method for tracing.
 */
@BallerinaFunction(
        packageName = "ballerina.tracing",
        functionName = "finishSpan",
        returnType = {@ReturnType(type = TypeKind.BOOLEAN)},
        isPublic = true
)

public class FinishSpan extends AbstractNativeFunction {

    @Override
    public BValue[] execute(Context context) {
        String spanId = getStringArgument(context, 0);
        List<Span> spanList = SpanHolder.getInstance().onFinishSpan("xxxxxxx", spanId);
        OpenTracerFactory.getInstance().finishSpan(spanList);
        return getBValues(new BBoolean(true));
    }

}