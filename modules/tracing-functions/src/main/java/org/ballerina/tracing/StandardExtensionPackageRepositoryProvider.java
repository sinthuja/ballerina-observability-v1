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

import org.ballerinalang.annotation.JavaSPIService;
import org.ballerinalang.repository.PackageRepository;
import org.ballerinalang.repository.fs.ClasspathPackageRepository;
import org.ballerinalang.spi.ExtensionPackageRepositoryProvider;

/**
 * This is the class which registers the functions defined with the ballerina.
 */
@JavaSPIService("org.ballerinalang.spi.ExtensionPackageRepositoryProvider")
public class StandardExtensionPackageRepositoryProvider implements ExtensionPackageRepositoryProvider {

private static final String JAR_SYSTEM_LIB_LOCATION = "/META-INF/natives/";

@Override
    public PackageRepository loadRepository() {
        return new ClasspathPackageRepository(this.getClass(), JAR_SYSTEM_LIB_LOCATION);
    }
}
