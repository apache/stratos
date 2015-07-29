/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.stratos.integration.tests;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParser;
import org.apache.commons.lang.StringUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;

/**
 * Util class
 */
public class StratosArtifactsUtils {

    public String getJsonStringFromFile(String filePath) throws FileNotFoundException {
        JsonParser parser = new JsonParser();
        Object object = parser.parse(new FileReader(getResourcesFolderPath() + filePath));
        GsonBuilder gsonBuilder = new GsonBuilder();
        Gson gson = gsonBuilder.create();
        String content = gson.toJson(object);
        return content;

    }

    /**
     * Get resources folder path
     * @return
     */
    private String getResourcesFolderPath() {
        String path = getClass().getResource("/").getPath();
        return StringUtils.removeEnd(path, File.separator);
    }
}
