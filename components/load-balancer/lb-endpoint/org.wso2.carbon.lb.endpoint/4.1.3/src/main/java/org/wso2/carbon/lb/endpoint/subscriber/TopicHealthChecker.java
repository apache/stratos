/*
*  Copyright (c) 2005-2011, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
package org.wso2.carbon.lb.endpoint.subscriber;

import javax.jms.TopicSubscriber;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * This health checker runs forever, and is responsible for re-establishing a connection
 * between ELB and CC.
 */
public class TopicHealthChecker implements Runnable{
    
    private static final Log log = LogFactory.getLog(TopicHealthChecker.class);
    private String topicName;
    private TopicSubscriber subscriber;

    public TopicHealthChecker(String topicName, TopicSubscriber subscriber) {
        this.topicName = topicName;
        this.subscriber = subscriber;
    }
    
    @Override
    public void run() {
        log.info("Topic Health Checker is running... ");

        while (true) {
            try {
                subscriber.getTopic();
                
                // health checker runs in every 30s
                Thread.sleep(30000);

            } catch (Exception e) {
                // implies connection is not established
                // sleep for 5s and retry
                try {
                    log.info("Health checker failed and will retry to establish a connection after a 5s.");
                    Thread.sleep(5000);
                    break;
                } catch (InterruptedException ignore) {
                }
            }

        }

        TopologySubscriber.subscribe(topicName);

    }

}
