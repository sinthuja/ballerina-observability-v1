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
package org.ballerina.tracing.core.scope;

import io.opentracing.Scope;
import io.opentracing.Span;

/**
 * The scope implementation which can be cloned.
 */
public class ClonableScope implements Scope {
    private final ClonableThreadLocalScopeManager scopeManager;
    private final Span wrapped;
    private final boolean finishOnClose;
    private final ClonableScope toRestore;

    ClonableScope(ClonableThreadLocalScopeManager scopeManager, Span wrapped, boolean finishOnClose) {
        this.scopeManager = scopeManager;
        this.wrapped = wrapped;
        this.finishOnClose = finishOnClose;
        this.toRestore = scopeManager.tlsScope.get();
        scopeManager.tlsScope.set(this);
    }

    public void close() {
        if (this.scopeManager.tlsScope.get() == this) {
            if (this.finishOnClose) {
                this.wrapped.finish();
            }

            this.scopeManager.tlsScope.set(this.toRestore);
        }
    }

    public Span span() {
        return this.wrapped;
    }

    public boolean getFinishOnClose() {
        return this.finishOnClose;
    }

    public ClonableScope copy() {
        return new ClonableScope(this.scopeManager, this.wrapped, this.getFinishOnClose());
    }

}
