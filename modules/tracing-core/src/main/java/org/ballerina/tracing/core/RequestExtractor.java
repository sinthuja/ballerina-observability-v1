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

import io.opentracing.propagation.TextMap;

import java.util.Iterator;
import java.util.Map;

/**
 * Extractor that goes through the backage items.
 */
public class RequestExtractor implements TextMap {

    private Map<String, String> headers;
    private Iterator<Map.Entry<String, String>> iterator;

    public RequestExtractor(Map<String, String> headers) {
        this.headers = headers;
    }

    public RequestExtractor(Iterator<Map.Entry<String, String>> headers) {
        this.iterator = headers;
    }

    @Override
    public Iterator<Map.Entry<String, String>> iterator() {
        if (iterator == null) {
            return this.headers.entrySet().iterator();
        } else {
            return iterator;
        }
    }

    @Override
    public void put(String s, String s1) {
        throw new UnsupportedOperationException("This class should be used only with Tracer.extract()!");
    }
}
