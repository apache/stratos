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

package org.apache.stratos.cartridge.agent;

import com.google.gson.Gson;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;

/**
 * Cartridge agent main class.
 */
public class Main {
    private static final Log log = LogFactory.getLog(Main.class);

    public static void main(String[] args) {
        try {
            if ((args != null) && (args.length == 1)) {
                Runnable runnable = new CartridgeAgent(args[0]);
                Thread thread = new Thread(runnable);
                thread.start();
            } else {
                printInvalidArgs();
            }
        } catch (Exception e) {
            printInvalidArgs();
        }
    }

    private static void printInvalidArgs() {
        if (log.isErrorEnabled()) {
            log.error("Arguments are not valid. Cartridge agent could not be started.");
            log.error("Expected: user-data-file-path");
        }
    }
}
