/**
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at

 *  http://www.apache.org/licenses/LICENSE-2.0

 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */
package org.apache.stratos.load.balancer.conf.structure;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.load.balancer.conf.util.Constants;

/**
 * Responsible for build up a Node object from a given content.
 * Every closing brace should be in a new line.
 */
public class NodeBuilder {

    private static final Log log = LogFactory.getLog(NodeBuilder.class);

    /**
     * Construct a node structure for the given content.
     *
     *            # comment
     *            property_1 value1;
     *            property_2 value2;
     *
     *            node1 { # comment
     *              property_3 value3;
     *              property_4 value4;
     *
     *              node2 {
     *                  property_5 value5;
     *              }
     *            }
     *
     * @return fully constructed root node
     */
    public static Node buildNode(String content) {
        return buildNode(null, content);
    }

    private static Node buildNode(Node node, String content) {

        if (content == null || content.isEmpty()) {
            return node;
        }

        String[] lines = content.split("\n");

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();

            // Skip line comments
            if (!line.startsWith(Constants.NGINX_COMMENT)) {

                // Skip inline comments
                if (line.contains(Constants.NGINX_COMMENT)) {
                    line = line.substring(0, line.indexOf(Constants.NGINX_COMMENT));
                }

                // A node is detected and it is not a variable starting from $
                if (line.contains(Constants.NGINX_NODE_START_BRACE) && !line.contains(Constants.NGINX_VARIABLE)) {

                    try {
                        Node childNode = new Node();
                        childNode.setName(line.substring(0, line.indexOf(Constants.NGINX_NODE_START_BRACE)).trim());

                        StringBuilder sb = new StringBuilder();
                        int matchingBraceTracker = 1;

                        while (!line.contains(Constants.NGINX_NODE_END_BRACE) || matchingBraceTracker != 0) {
                            i++;
                            if (i == lines.length) {
                                break;
                            }
                            line = lines[i];
                            sb.append(line + "\n");

                            if (line.contains(Constants.NGINX_NODE_START_BRACE)) {
                                matchingBraceTracker++;
                            }
                            if (line.contains(Constants.NGINX_NODE_END_BRACE)) {
                                matchingBraceTracker--;
                            }
                        }

                        childNode = buildNode(childNode, sb.toString());
                        if (node == null) {
                            node = childNode;
                        } else {
                            node.appendChild(childNode);
                        }

                    } catch (Exception e) {
                        String msg = "Malformed element found in configuration: [" + i + "] \n";
                        log.error(msg, e);
                        throw new RuntimeException(msg + line, e);
                    }

                } else {
                    if (!line.isEmpty() && !Constants.NGINX_NODE_END_BRACE.equals(line)) {
                        // Add property
                        String[] prop = line.split(Constants.NGINX_SPACE_REGEX);
                        String key = prop[0].replace(":", "");
                        String value = line.substring(prop[0].length(), line.indexOf(Constants.NGINX_LINE_DELIMITER)).trim();
                        try {
                            node.addProperty(key, value);
                        } catch (Exception e) {
                            String msg = "Malformed property found in configuration: [" + i + "] \n";
                            log.error(msg, e);
                            throw new RuntimeException(msg + line, e);
                        }
                    }
                }
            }
        }
        return node;
    }
}
