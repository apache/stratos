/*                                                                             
 * Copyright 2004,2005 The Apache Software Foundation.                         
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
package org.wso2.carbon.mediator.autoscale.lbautoscale.util;

/**
 * Constants
 */
public final class AutoscaleConstants {
    public static final String REQUEST_ID = "request.id";
    public static final String APP_DOMAIN_CONTEXTS = "autoscale.app.domain.contexts";
    public static final String TARGET_DOMAIN = "autoscale.target.domain";
    public static final String TARGET_SUB_DOMAIN = "autoscale.target.sub.domain";
    public static final String LOAD_BALANCER_CONFIG = "loadbalancer.conf";
    
    /**
     * we use this to track the changes happen to request token list lengths.
     */
    public static final String IS_TOUCHED = "is_touched";
    
    /**
     * We'll check whether a server is started up in every this much of time.
     * (in milliseconds)
     */
    public static final int SERVER_START_UP_CHECK_TIME = 30000;
    
    /**
     * We'll check whether an instance is left the cluster in every this much of time.
     * (in milliseconds)
     */
    public static final int INSTANCE_REMOVAL_CHECK_TIME = 5000;

    /**
     * Name of the EC2 instance tag which if set on an instance, the autoscaler will not
     * terminate such instance
     */
    public static final String AVOID_TERMINATION = "avoidTermination";

    public static enum InstanceState {
        RUNNING("running"), PENDING("pending"), TERMINATED("terminated"), SHUTTING_DOWN("shutting-down");

        private String state;

        InstanceState(String state) {
            this.state = state;
        }

        public String getState() {
            return state;
        }
    }

}
