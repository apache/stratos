/*
*  Copyright (c) 2005-2010, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
*
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
*/
package org.wso2.carbon.ui.deployment.beans;

import java.io.Serializable;

/**
 * Represents a context in UI Framework
 */
public class Context implements Serializable {
    private String contextId;
    private String contextName;
    private String protocol;
    private String description;

    public void setContextId(String contextId) {
        this.contextId = contextId;
    }

    public void setContextName(String contextName) {
        this.contextName = contextName;
    }

    public void setDescription(String description) {
        this.description = description;
    }


    public String getContextId() {
        return contextId;
    }

    public String getContextName() {
        return contextName;
    }

    public String getDescription() {
        return description;
    }

    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }
}
