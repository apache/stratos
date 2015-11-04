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

package org.apache.stratos.aws.extension.persistence.dto;

import java.io.Serializable;

public class LBInfoDTO implements Serializable {

    private static final long serialVersionUID = -3788551425235723608L;
    private String name;
    private String clusterId;
    private String region;

    public LBInfoDTO(String name, String clusterId, String region) {
        this.name = name;
        this.clusterId = clusterId;
        this.region = region;
    }

    public String getName() {
        return name;
    }

    public String getClusterId () {
        return clusterId;
    }

    public String getRegion() {
        return region;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        LBInfoDTO lbInfoDTO = (LBInfoDTO) o;

        if (!name.equals(lbInfoDTO.name)) return false;
        if (!clusterId.equals(lbInfoDTO.clusterId)) return false;
        return region.equals(lbInfoDTO.region);

    }

    @Override
    public int hashCode() {
        int result = name.hashCode();
        result = 31 * result + clusterId.hashCode();
        result = 31 * result + region.hashCode();
        return result;
    }
}
