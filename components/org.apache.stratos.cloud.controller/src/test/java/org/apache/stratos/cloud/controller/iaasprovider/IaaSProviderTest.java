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

package org.apache.stratos.cloud.controller.iaasprovider;

import junit.framework.TestCase;
import org.apache.axiom.om.OMElement;
import org.apache.stratos.cloud.controller.config.CloudControllerConfig;
import org.apache.stratos.cloud.controller.config.parser.CloudControllerConfigParser;
import org.apache.stratos.cloud.controller.domain.IaasProvider;
import org.apache.stratos.cloud.controller.iaases.Iaas;
import org.apache.stratos.common.util.AxiomXpathParserUtil;

import java.io.File;
import java.util.ArrayList;

public class IaaSProviderTest extends TestCase {

    private static final String IAAS_TYPE_EC2 = "ec2";
    private static final String IAAS_PROVIDER_EC2 = "aws-ec2";
    private static final String EC2_IAAS_IMPL_CLASS = "org.apache.stratos.cloud.controller.iaases.ec2.EC2Iaas";

    private IaasProvider iaasProvider;
    private File xmlFile = new File("src/test/resources/cloud-controller-ec2-iaas.xml");
    private OMElement docElt;

    protected void setUp() throws Exception {
        super.setUp();
        docElt = AxiomXpathParserUtil.parse(xmlFile);
        CloudControllerConfigParser.parse(docElt);
        assertEquals(IAAS_TYPE_EC2, CloudControllerConfig.getInstance().
                getIaasProvider(IAAS_TYPE_EC2).getType());
        assertEquals(EC2_IAAS_IMPL_CLASS, CloudControllerConfig.getInstance().
                getIaasProvider(IAAS_TYPE_EC2).getClassName());
        assertEquals(IAAS_PROVIDER_EC2, CloudControllerConfig.getInstance().
                getIaasProvider(IAAS_TYPE_EC2).getProvider());
    }

    public void testComputeService () {
        iaasProvider = CloudControllerConfig.getInstance().getIaasProvider(IAAS_TYPE_EC2);
        iaasProvider.getIaas();
        assertNotNull(iaasProvider.getComputeService());
    }

    protected void tearDown() throws Exception {
        CloudControllerConfig.getInstance().setIaasProviders(new ArrayList<IaasProvider>());
        iaasProvider = null;
        super.tearDown();
    }
}
