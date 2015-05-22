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

package org.apache.stratos.common.util;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Map;

/**
 * A utility class for executing shell commands.
 */
public class CommandUtils {
    private static final Log log = LogFactory.getLog(CommandUtils.class);
    private static final String NEW_LINE = System.getProperty("line.separator");

    public static String executeCommand(String command) throws IOException {
        String line;
        Runtime r = Runtime.getRuntime();
        if (log.isDebugEnabled()) {
            log.debug("command = " + command);
        }
        Process p = r.exec(command);

        StringBuilder output = new StringBuilder();
        BufferedReader in = new BufferedReader(new InputStreamReader(p.getInputStream()));
        while ((line = in.readLine()) != null) {
            if (log.isDebugEnabled()) {
                log.debug("output = " + line);
            }
            output.append(line).append(NEW_LINE);
        }
        StringBuilder errors = new StringBuilder();
        BufferedReader error = new BufferedReader(new InputStreamReader(p.getErrorStream()));
        while ((line = error.readLine()) != null) {
            if (log.isDebugEnabled()) {
                log.debug("error = " + line);
            }
            errors.append(line).append(NEW_LINE);
        }
        if (errors.length() > 0) {
            throw new RuntimeException("Command execution failed: " + NEW_LINE + errors.toString());
        }

        return output.toString();
    }

	public static String executeCommand(String[] command) throws IOException {
		String line;
		Runtime r = Runtime.getRuntime();
		if (log.isDebugEnabled()) {
			log.debug("command = " + command);
		}
		Process p = r.exec(command);

		StringBuilder output = new StringBuilder();
		BufferedReader in = new BufferedReader(new InputStreamReader(p.getInputStream()));
		while ((line = in.readLine()) != null) {
			if (log.isDebugEnabled()) {
				log.debug("output = " + line);
			}
			output.append(line).append(NEW_LINE);
		}
		StringBuilder errors = new StringBuilder();
		BufferedReader error = new BufferedReader(new InputStreamReader(p.getErrorStream()));
		while ((line = error.readLine()) != null) {
			if (log.isDebugEnabled()) {
				log.debug("error = " + line);
			}
			errors.append(line).append(NEW_LINE);
		}
		if (errors.length() > 0) {
			throw new RuntimeException("Command execution failed: " + NEW_LINE + errors.toString());
		}

		return output.toString();
	}
    public static String executeCommand(String command, Map<String, String> envParameters) throws IOException {
        String line;
        ProcessBuilder pb = new ProcessBuilder(command);
        Map<String, String> env = pb.environment();
        env.putAll(envParameters);

        Process p = pb.start();
        StringBuilder output = new StringBuilder();
        BufferedReader in = new BufferedReader(new InputStreamReader(p.getInputStream()));
        while ((line = in.readLine()) != null) {
            if (log.isDebugEnabled()) {
                log.debug("output = " + line);
            }
            output.append(line).append(NEW_LINE);
        }
        StringBuilder errors = new StringBuilder();
        BufferedReader error = new BufferedReader(new InputStreamReader(p.getErrorStream()));
        while ((line = error.readLine()) != null) {
            if (log.isDebugEnabled()) {
                log.debug("error = " + line);
            }
            errors.append(line).append(NEW_LINE);
        }
        if (errors.length() > 0) {
            throw new RuntimeException("Command execution failed: " + NEW_LINE + errors.toString());
        }
        return output.toString();
    }
}
