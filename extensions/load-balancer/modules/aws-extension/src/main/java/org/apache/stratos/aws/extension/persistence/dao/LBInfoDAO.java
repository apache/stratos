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

package org.apache.stratos.aws.extension.persistence.dao;

import org.apache.stratos.aws.extension.persistence.dto.LBInfoDTO;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

public class LBInfoDAO implements Serializable {

    private static final long serialVersionUID = 6429853257198804560L;
    private final Set<LBInfoDTO> lbInformation;

    public LBInfoDAO() {
        lbInformation = new HashSet<>();
    }

    public void add (LBInfoDTO lbInfo) {
        lbInformation.add(lbInfo);
    }

    public void remove (LBInfoDTO lbInfo) {
        lbInformation.remove(lbInfo);
    }

    public void clear () {
        lbInformation.clear();
    }

    public Set<LBInfoDTO> get () {
        return lbInformation;
    }
}