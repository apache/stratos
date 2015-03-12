/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jclouds.vcloud.domain;

import java.net.URI;

/**
 * The DiskAttachOrDetachParams element forms the body of a disk attach/detach request
 * see http://pubs.vmware.com/vchs/index.jsp?topic=%2Fcom.vmware.vcloud.api.reference.doc_56%2Fdoc%2Ftypes%2FDiskAttachOrDetachParamsType.html
 */
public class DiskAttachOrDetachParams {
    protected final URI href;
    protected final String type;
    protected final String id;
    protected final String name;
    protected final Integer BusNumber;
    protected final Integer UnitNumber;

    /**
     * "application/vnd.vmware.vcloud.disk+xml"
     */
    //TODO: is this the right place?
    public static final String DISK_XML = "application/vnd.vmware.vcloud.disk+xml";

    public DiskAttachOrDetachParams(URI href) {
        this.href = href;
        this.type = DISK_XML;
        this.id = null;
        this.name = null;
        this.BusNumber = null;
        this.UnitNumber = null;
    }

    public DiskAttachOrDetachParams(URI href, String name,
                                    String id, Integer BusNumber, Integer UnitNumber) {
        this.href = href;
        this.type = DISK_XML;
        this.id = id;
        this.name = name;
        this.BusNumber = BusNumber;
        this.UnitNumber = UnitNumber;
    }

    /**
     * get href
     * @return href
     */
    public URI getHref() {
        return href;
    }

    /**
     * get type
     * @return type
     */
    public String getType() {
        return type;
    }

    /**
     * get id
     * @return id
     */
    public String getId() {
        return id;
    }

    /**
     * get name
     * @return name
     */
    public String getName() {
        return name;
    }

    /**
     * get bus nr
     * @return bus nr
     */
    public Integer getBusNumber() {
        return BusNumber;
    }

    /**
     * get unit number
     * @return unit number
     */
    public Integer getUnitNumber() {
        return UnitNumber;
    }

    @Override
    public String toString() {
        return "[href=" + getHref() + ", type=" + getType() + ", id=" + getId() + ", name=" + getName()
                + ", BusNumber=" + getBusNumber() + ", UnitNumber=" + getUnitNumber() + "]";
    }

    @Override
    public int hashCode() {
        int prime = 31;
        int result = 1;
        result = prime * result + ((href == null) ? 0 : href.hashCode());
        result = prime * result + ((type == null) ? 0 : type.hashCode());
        result = prime * result + ((id == null) ? 0 : id.hashCode());
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        result = prime * result + ((BusNumber == null) ? 0 : BusNumber.hashCode());
        result = prime * result + ((UnitNumber == null) ? 0 : UnitNumber.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        DiskAttachOrDetachParams other = (DiskAttachOrDetachParams)obj;

        if (href == null) {
            if (other.href != null)
                return false;
        } else if (!href.equals(other.href)) {
            return false;
        }
        if (type == null) {
            if (other.type != null)
                return false;
        } else if (!type.equals(other.type)) {
            return false;
        }
        if (id == null) {
            if (other.id != null)
                return false;
        } else if (!id.equals(other.id)) {
            return false;
        }
        if (name == null) {
            if (other.name != null)
                return false;
        } else if (!name.equals(other.name)) {
            return false;
        }
        if (BusNumber == null) {
            if (other.BusNumber != null)
                return false;
        } else if (!BusNumber.equals(other.BusNumber)) {
            return false;
        }
        if (UnitNumber == null) {
            if (other.UnitNumber != null)
                return false;
        } else if (!UnitNumber.equals(other.UnitNumber)) {
            return false;
        }
        return true;
    }
}
