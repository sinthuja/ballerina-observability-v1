package org.ballerina.tracing.extension.datadog;

/*
*  Copyright (c) 2017, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

/**
 * This is the constants class that defines all the constants that are used by the {@link OpenTracingExtension}
 */
public class Constants {
    static final String TRACER_NAME = "datadog";

    static final String SERVICE_NAME = "service.name";
    static final String WRITER_TYPE = "writer.type";
    static final String AGENT_HOST = "agent.host";
    static final String AGENT_PORT = "agent.port";
    static final String PRIORITY_SAMPLING = "priority.sampling";

    static final String DEFAULT_SERVICE_NAME = "ballerina-service-1";
    static final String DEFAULT_WRITER_TYPE = "DDAgentWriter";
    static final String DEFAULT_AGENT_HOST = "localhost";
    static final int DEFAULT_AGENT_PORT = 8126;
    static final boolean DEFAULT_PRIORITY_SAMPLING = false;
}
