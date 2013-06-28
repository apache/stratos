/*
 * Copyright (c) 2008, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
package org.wso2.stratos.manager.dashboard.ui.utils;

import java.util.ArrayList;
import java.util.List;

public class Util {
    public static List<String> getNewlyActivatedServices(List<String> oldActivateServices,
                                                  List<String> newActivateServices) {
        List<String> newlyActivatedServices = new ArrayList<String>();
        for (String service: newActivateServices) {
            if (!oldActivateServices.contains(service)) {
                newlyActivatedServices.add(service);
            }
        }
        return newlyActivatedServices;
    }
    public static List<String> getNewlyDeactivatedServices(List<String> oldActivateServices,
                                                  List<String> newActivateServices) {
        List<String> newlyDeactivatedServices = new ArrayList<String>();
        for (String service: oldActivateServices) {
            if (!newActivateServices.contains(service)) {
                newlyDeactivatedServices.add(service);
            }
        }
        return newlyDeactivatedServices;
    }

   
}

