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

import org.ballerina.tracing.core.config.ConfigLoader;
import org.ballerina.tracing.core.config.OpenTracingConfig;
import org.ballerina.tracing.core.config.TracerConfig;
import org.ballerina.tracing.core.config.TracingDepth;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.File;
import java.net.URL;

/**
 * This is the unit test to validate the configurations loading.
 */
public class ConfigurationTest {

    @Test
    public void loadConfigTest() {
        String tracerName = "test";
        URL resource = ConfigurationTest.class.getResource(File.separator + "tracing-config.yaml");
        OpenTracingConfig config = ConfigLoader.load(resource.getFile());
        Assert.assertTrue(config != null, "No config loaded..");
        Assert.assertEquals(config.getDepth(), TracingDepth.FUNCTION_LEVEL);
        Assert.assertEquals(config.getTracers().size(), 1);
        Assert.assertTrue(config.getTracer(tracerName) != null, "There should be a tracer registered in the name of "
                + tracerName + "but it's missing");
        TracerConfig tracerConfig = config.getTracer("test");
        Assert.assertEquals(tracerConfig.getConfiguration().size(), 2);
        Assert.assertEquals(tracerConfig.getConfiguration().get("reporter.hostname"), "testhost");
        Assert.assertEquals(tracerConfig.getConfiguration().get("reporter.port"), 1234);
    }
}
