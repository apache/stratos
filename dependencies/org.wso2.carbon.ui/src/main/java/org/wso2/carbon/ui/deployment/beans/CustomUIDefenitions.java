/*
 * Copyright (c) 2006, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.wso2.carbon.ui.deployment.beans;

import java.util.HashMap;
import java.util.Map;

/**
 * Stores the custom UI defintions in the form of media type -> UI (jsp) path. Object of this class
 * filled with currently available custom UI will be available in the servlet context.
 */
public class CustomUIDefenitions {

    public static final String CUSTOM_UI_DEFENITIONS = "customUIDefinitions";

    private Map <String, String> customViewUIMap = new HashMap <String, String> ();
    private Map <String, String> customAddUIMap = new HashMap <String, String> ();

    public String getCustomViewUI(String mediaType) {
        return customViewUIMap.get(mediaType);
    }

    public void addCustomViewUI(String mediaType, String uiPath) {
        customViewUIMap.put(mediaType, uiPath);
    }

    public String getCustomAddUI(String mediaType) {
        return customAddUIMap.get(mediaType);
    }

    public void addCustomAddUI(String mediaType, String uiPath) {
        customAddUIMap.put(mediaType, uiPath);
    }
}
