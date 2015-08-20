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
package org.jclouds.vcloud.binders;

import com.google.common.base.Throwables;
import com.google.inject.Inject;
import com.jamesmurty.utils.XMLBuilder;
import org.jclouds.http.HttpRequest;
import org.jclouds.rest.MapBinder;
import org.jclouds.rest.binders.BindToStringPayload;
import org.jclouds.rest.internal.GeneratedHttpRequest;
import org.jclouds.vcloud.domain.DiskAttachOrDetachParams;

import javax.inject.Named;
import javax.inject.Singleton;
import javax.xml.parsers.FactoryConfigurationError;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import java.util.Map;
import java.util.Properties;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static org.jclouds.vcloud.reference.VCloudConstants.PROPERTY_VCLOUD_XML_NAMESPACE;
import static org.jclouds.vcloud.reference.VCloudConstants.PROPERTY_VCLOUD_XML_SCHEMA;

/**
 * Created by mblokzij on 29/10/2014.
 */
@Singleton
public class BindDiskAttachOrDetachParamsToXmlPayload implements MapBinder {

    protected final String ns;
    protected final String schema;

    protected final BindToStringPayload stringBinder;

    @Inject
    public BindDiskAttachOrDetachParamsToXmlPayload(BindToStringPayload stringBinder,
                                                    @Named(PROPERTY_VCLOUD_XML_NAMESPACE) String ns, @Named(PROPERTY_VCLOUD_XML_SCHEMA) String schema) {
        this.ns = ns;
        this.schema = schema;
        this.stringBinder = stringBinder;
    }

    @Override
    public <R extends HttpRequest> R bindToRequest(R request, Map<String, Object> postParams) {
        checkArgument(checkNotNull(request, "request") instanceof GeneratedHttpRequest,
                "this binder is only valid for GeneratedHttpRequests!");
        DiskAttachOrDetachParams params = (DiskAttachOrDetachParams) checkNotNull(postParams.remove("params"), "params");
        XMLBuilder diskAttachOrDetachParams;
        String xml = null;
        try {
            diskAttachOrDetachParams = XMLBuilder.create("DiskAttachOrDetachParams").a("xmlns", ns);
            XMLBuilder disk = diskAttachOrDetachParams.e("Disk");
            if (params.getHref() != null) {
                disk.a("href", params.getHref().toString());
            }
            if (params.getId() != null) {
                disk.a("id", params.getId().toString());
            }
            if (params.getName() != null) {
                disk.a("name", params.getName());
            }
            if (params.getType() != null) {
                disk.a("type", params.getType());
            }
            if (params.getBusNumber() != null) {
                diskAttachOrDetachParams.e("BusNumber").t(params.getBusNumber().toString());
            }
            if (params.getUnitNumber() != null) {
                diskAttachOrDetachParams.e("UnitNumber").t(params.getUnitNumber().toString());
            }

            xml = diskAttachOrDetachParams.asString();
        } catch (Exception e) {
            Throwables.propagate(e);
        }
        return stringBinder.bindToRequest(request, xml);
    }

    @Override
    public <R extends HttpRequest> R bindToRequest(R request, Object postParams) {
        throw new IllegalStateException("BindDiskAttachOrDetachParamsToXmlPayload needs parameters");
    }
}
