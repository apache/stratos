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

package org.apache.stratos.manager.composite.application.structure;

public class StartupOrder {

    private String start;

    private String after;

    public StartupOrder (String start, String after) {
        this.start = start;
        this.after = after;
    }

    public String getStart() {
        return start;
    }

    public void setStart(String start) {
        this.start = start;
    }

    public String getAfter() {
        return after;
    }

    public void setAfter(String after) {
        this.after = after;
    }

    public boolean equals(Object other) {

        if(this == other) {
            return true;
        }
        if(!(other instanceof StartupOrder)) {
            return false;
        }

        StartupOrder that = (StartupOrder)other;
        return this.start.equals(that.start) && this.after.equals(that.after);
    }

    public int hashCode () {

        return start.hashCode() + after.hashCode();
    }
}
