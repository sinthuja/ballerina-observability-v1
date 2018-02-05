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
package org.ballerina.platform.tracing.core.config;

import org.ballerina.platform.tracing.core.Constants;
import org.ballerinalang.config.ConfigRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

public class ConfigLoader {

    private static final Logger logger = LoggerFactory.getLogger(ConfigLoader.class);

    private ConfigLoader() {

    }

    public static OpenTracingConfig load() {
        String configLocation = ConfigRegistry.getInstance().getGlobalConfigValue(Constants.BALLERINA_TRACE_CONFIG_KEY);
        if (configLocation == null) {
            return null;
        } else {
            File initialFile = new File(configLocation);
            try {
                InputStream targetStream = new FileInputStream(initialFile);
                return new Yaml().loadAs(targetStream, OpenTracingConfig.class);
            } catch (FileNotFoundException e) {
                logger.error("Unable to load the file provided with global configuration - " +
                        Constants.BALLERINA_TRACE_CONFIG_KEY, e);
                return null;
            }
        }
    }
}
