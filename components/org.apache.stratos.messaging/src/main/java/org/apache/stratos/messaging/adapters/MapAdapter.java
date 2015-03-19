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

package org.apache.stratos.messaging.adapters;

import javax.xml.bind.annotation.adapters.XmlAdapter;
import java.util.HashMap;
import java.util.Map;

public class MapAdapter<S, T> extends XmlAdapter<MapType, Map<S, T>> {

    @Override
    public MapType marshal(Map<S, T> v) throws Exception {

        MapType mapType = new MapType();

        for (Map.Entry entry : v.entrySet()) {
            MapEntryType myMapEntryType = new MapEntryType();
            myMapEntryType.key = entry.getKey();
            myMapEntryType.value = entry.getValue();
            mapType.entry.add(myMapEntryType);
        }
        return mapType;
    }

    @Override
    public Map<S, T> unmarshal(MapType v) throws Exception {

        Map hashMap = new HashMap();

        for (MapEntryType mapEntryType : v.entry) {
            hashMap.put(mapEntryType.key, mapEntryType.value);
        }
        return hashMap;
    }
}
