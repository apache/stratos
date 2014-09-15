/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.stratos.cloud.controller.application.parser;

import org.apache.stratos.messaging.domain.topology.StartupOrder;

import java.util.HashSet;
import java.util.Set;

public class ParserUtils {

    public static Set<StartupOrder> convert (org.apache.stratos.cloud.controller.pojo.StartupOrder [] startupOrderArr) {

        Set<StartupOrder> startupOrders = new HashSet<StartupOrder>();

        if (startupOrderArr == null) {
            return startupOrders;
        }

        for (int i = 0; i < startupOrderArr.length ; i++) {
            startupOrders.add(new StartupOrder(startupOrderArr[i].getStart(), startupOrderArr[i].getAfter()));
        }

        return startupOrders;
    }
}
