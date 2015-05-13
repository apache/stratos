/*
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 *
*/
package org.apache.stratos.cli.completer;

import jline.console.completer.FileNameCompleter;
import org.apache.stratos.cli.utils.CliConstants;

import java.io.File;
import java.util.List;

public class StratosFileNameCompleter extends FileNameCompleter {

    @Override
    public int complete(String buf, int arg1, List<CharSequence> candidates) {

        String buffer = (buf == null) ? "" : buf;
        String subString = null;
        int index = buf.lastIndexOf("--" + CliConstants.RESOURCE_PATH_LONG_OPTION);
        if (buf.length() >= index + 16) {
            subString = buf.substring(index + 16);
        }

        String translated = (subString == null || subString.isEmpty()) ? buf
                : subString;
        if (translated.startsWith("~" + File.separator)) {
            translated = System.getProperty("user.home")
                    + translated.substring(1);
        } else if (translated.startsWith("." + File.separator)) {
            translated = new File("").getAbsolutePath() + File.separator
                    + translated;
        }

        File f = new File(translated);

        final File dir;

        if (translated.endsWith(File.separator)) {
            dir = f;
        } else {
            dir = f.getParentFile();
        }

        final File[] entries = (dir == null) ? new File[0] : dir.listFiles();

        return matchFiles(buffer, translated, entries, candidates);

    }

}
