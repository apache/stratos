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
package org.wso2.carbon.billing.core.jdbc;

import java.sql.Connection;

public class Transaction {

    private static ThreadLocal<Boolean> tStarted = new ThreadLocal<Boolean>() {
        protected Boolean initialValue() {
            return false;
        }
    };

    private static ThreadLocal<Connection> tConnection = new ThreadLocal<Connection>() {
        protected Connection initialValue() {
            return null;
        }
    };

    // this is a property that will be specific to embedded registry
    private static ThreadLocal<Integer> tNestedDepth = new ThreadLocal<Integer>() {
        protected Integer initialValue() {
            return 0;
        }
    };

    private static ThreadLocal<Boolean> tRollbacked = new ThreadLocal<Boolean>() {
        protected Boolean initialValue() {
            return false;
        }
    };

    public static boolean isStarted() {
        if (tStarted.get() == null) {
            return false;
        }

        return tStarted.get();
    }

    public static void setStarted(boolean started) {
        tStarted.set(started);
    }

    public static Connection getConnection() {
        return tConnection.get();
    }

    public static void setConnection(Connection connection) {
        if (connection != null) {
            tStarted.set(true);
        }
        tConnection.set(connection);
    }

    public static void removeConnection() {
        tStarted.remove();
        tConnection.remove();
    }

    // the following two methods will increment and decrement
    // the transaction depth of the current session
    public static void incNestedDepth() {
        int transactionDepth = tNestedDepth.get().intValue();
        if (transactionDepth == 0) {
            tStarted.set(true);
            tRollbacked.set(false);
        }
        transactionDepth++;
        tNestedDepth.set(transactionDepth);
    }

    public static void decNestedDepth() {
        int transactionDepth = tNestedDepth.get().intValue();
        transactionDepth--;
        tNestedDepth.set(transactionDepth);
        if (transactionDepth == 0) {
            tStarted.set(false);
        }
    }

    public static int getNestedDepth() {
        return tNestedDepth.get().intValue();
    }


    public static boolean isRollbacked() {
        if (tRollbacked.get() == null) {
            return false;
        }

        return tRollbacked.get();
    }

    public static void setRollbacked(boolean rollbacked) {
        tRollbacked.set(rollbacked);
    }

}
