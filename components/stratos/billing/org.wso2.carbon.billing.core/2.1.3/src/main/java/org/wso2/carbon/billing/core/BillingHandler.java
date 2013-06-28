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
package org.wso2.carbon.billing.core;

import java.util.Map;

public interface BillingHandler {
    public void init(Map<String, String> handlerConfig) throws BillingException;

    /**
     * Performs the tasks needed to generate bill, sending bills, etc. 
     *
     * @param handlerContext
     * @throws BillingException
     */
    public void execute(BillingEngineContext handlerContext) throws BillingException;
}
