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
import com.google.inject.name.Named;
import com.jamesmurty.utils.XMLBuilder;
import org.jclouds.http.HttpRequest;
import org.jclouds.rest.MapBinder;
import org.jclouds.rest.binders.BindToStringPayload;
import org.jclouds.rest.internal.GeneratedHttpRequest;
import org.jclouds.vcloud.domain.ovf.VCloudNetworkAdapter;

import javax.inject.Singleton;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import java.util.Map;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static org.jclouds.vcloud.reference.VCloudConstants.PROPERTY_VCLOUD_XML_NAMESPACE;
import static org.jclouds.vcloud.reference.VCloudConstants.PROPERTY_VCLOUD_XML_SCHEMA;

/**
 * Created by michiel on 17/11/2014.
 */
@Singleton
public class BindVCloudNetworkAdapterToXmlPayload implements MapBinder {

    protected final String ns;
    protected final String schema;

    protected final BindToStringPayload stringBinder;

    @Inject
    public BindVCloudNetworkAdapterToXmlPayload(BindToStringPayload stringBinder,
                                                @Named(PROPERTY_VCLOUD_XML_NAMESPACE) String ns,
                                                @Named(PROPERTY_VCLOUD_XML_SCHEMA) String schema) {
        this.ns = ns;
        this.schema = schema;
        this.stringBinder = stringBinder;
    }

    @Override
    public <R extends HttpRequest> R bindToRequest(R request, Map<String, Object> postParams) {
        checkArgument(checkNotNull(request, "request") instanceof GeneratedHttpRequest,
                "this binder is only valid for GeneratedHttpRequests!");

        Iterable<VCloudNetworkAdapter> networkCards = (Iterable<VCloudNetworkAdapter>) checkNotNull(postParams.remove("params"), "params");

        /*
         * The Iterable<VCloudNetworkAdapter> needs to be turned into a RasdItemList.
         */
        XMLBuilder rasdItemListBuilder;
        String xml = null;
        try {
            rasdItemListBuilder = XMLBuilder.create("RasdItemsList").a("xmlns", ns);
            //all sorts of other crazy XML attributes
            rasdItemListBuilder.a("xmlns:rasd", "http://schemas.dmtf.org/wbem/wscim/1/cim-schema/2/CIM_ResourceAllocationSettingData");
            rasdItemListBuilder.a("xmlns:xsi", "http://www.w3.org/2001/XMLSchema-instance");
            rasdItemListBuilder.a("type", "application/vnd.vmware.vcloud.rasdItemsList+xml");
            //rasdItemListBuilder.a("href", "https://<vcloud>/api/vApp/vm-<UUID>/virtualHardwareSection/networkCards");
            //rasdItemListBuilder.a("xsi:schemaLocation", "http://www.vmware.com/vcloud/v1.5 http://172.28.44.90/api/v1.5/schema/master.xsd http://schemas.dmtf.org/wbem/wscim/1/cim-schema/2/CIM_ResourceAllocationSettingData http://schemas.dmtf.org/wbem/wscim/1/cim-schema/2.22.0/CIM_ResourceAllocationSettingData.xsd");

            for (VCloudNetworkAdapter nic: networkCards) {
                /*
                 * NOTE: the order of these items IS important.
                 */
                XMLBuilder rasdItem = rasdItemListBuilder.elem("Item");
                if (nic.getAddress() != null)
                    rasdItem.elem("rasd:Address").text(nic.getAddress());
                if (nic.getAddressOnParent() != null)
                    rasdItem.elem("rasd:AddressOnParent").text(nic.getAddressOnParent());
                if (nic.isAutomaticAllocation() != null)
                    rasdItem.elem("rasd:AutomaticAllocation").text(String.valueOf(nic.isAutomaticAllocation()));

                //the connection handling is a little bit more involved.
                if (nic.getConnections().size() > 1) {
                    /*
                     * The IP address is an attribute of the <rasd:Connection /> element, and we only store
                     * 1 IP address for the whole NIC. It's not clear to me why the nic.getConnections() returns
                     * a list anyway.
                     */
                    throw new UnsupportedOperationException("Currently we only support 1 connection per NIC.");
                }
                if (nic.getConnections() != null) {
                    for (String connection: nic.getConnections()) {
                        XMLBuilder c = rasdItem.elem("rasd:Connection").a("xmlns:vcloud", ns);
                        if (nic.getIpAddress() != null)
                            c.a("vcloud:ipAddress", nic.getIpAddress());

                        c.a("vcloud:primaryNetworkConnection", String.valueOf(nic.isPrimaryNetworkConnection()));
                        if (nic.getIpAddressingMode() != null)
                            c.a("vcloud:ipAddressingMode", nic.getIpAddressingMode());
                        c.text(connection);
                    }
                }

                if (nic.getDescription() != null)
                    rasdItem.elem("rasd:Description").text(nic.getDescription());
                if (nic.getElementName() != null)
                    rasdItem.elem("rasd:ElementName").text(nic.getElementName());
                if (nic.getInstanceID() != null)
                    rasdItem.elem("rasd:InstanceID").text(nic.getInstanceID());
                if (nic.getResourceSubType() != null)
                    rasdItem.elem("rasd:ResourceSubType").text(nic.getResourceSubType());
                if (nic.getResourceType() != null)
                    rasdItem.elem("rasd:ResourceType").text(nic.getResourceType().value());

                //TODO: remaining items
            }
            xml = rasdItemListBuilder.asString();
        } catch (ParserConfigurationException e) {
            Throwables.propagate(e);
        } catch (TransformerException e) {
            Throwables.propagate(e);
        }

        return stringBinder.bindToRequest(request, xml);
    }

    @Override
    public <R extends HttpRequest> R bindToRequest(R request, Object postParams) {
        throw new IllegalStateException("BindVCloudNetworkAdapterToXmlPayload needs parameters");
    }
}
