/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.stratos.cartridge.agent.health.publisher;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Health publisher main class.
 */

public class Main {
    private static final Log log = LogFactory.getLog(Main.class);

    public static void main (String args[]) {
        try {
            if (log.isInfoEnabled()) {
                log.info("Health publisher started");
            }

            System.out.println("This is health stat publisher module");

            HealthPublisherClient client = new HealthPublisherClient();
            client.run();

            System.exit(0);

        } catch (Exception e) {
            if (log.isErrorEnabled()) {
                log.error("Could not publish health stats", e);
            }
        }

        System.exit(-1);
    }
}