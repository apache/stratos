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

package org.apache.stratos.deployment.synchronizer.git.util;

import org.apache.commons.io.FileUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.File;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * String Utility methods
 */
public class Utilities {

    private static final Log log = LogFactory.getLog(Utilities.class);

    /**
     * Searches for a match in a input String against a regex
     *
     * @param input input String
     * @param regex regex to match
     * @param group grouping,
     *
     * @return result of the match if found, else empty String
     */
    public static String getMatch (String input, String regex, int group) {

        String whitespaceRemovedJsonString = input.replaceAll("\\s+","");
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(whitespaceRemovedJsonString);
        if(!matcher.find())
            return "";
        else
            return matcher.group(group).trim();
    }

    /**
     * Deletes a folder structure recursively
     *
     * @param existingDir folder to delete
     */
    public static void deleteFolderStructure (File existingDir) {

        try {
            FileUtils.deleteDirectory(existingDir);

        } catch (IOException e) {
            log.error("Deletion of existing non-git repository structure failed");
            e.printStackTrace();
        }
    }
}
