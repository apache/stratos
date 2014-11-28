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
package org.apache.cartridge.autoscaler.service.axiom;

import java.io.File;

import org.apache.axiom.om.OMElement;
import org.apache.stratos.cloud.controller.util.AxiomXpathParserUtil;
import org.xml.sax.SAXParseException;

import junit.framework.TestCase;

public class AxiomValidationTest extends TestCase {
    
    File xmlSchemaCartridges = new File("src/main/resources/cartridges.xsd");
    File xmlSchemaCartridge = new File("src/main/resources/cartridge.xsd");
    private String dir = "src/test/resources/";

    public AxiomValidationTest(String name) {
        super(name);
    }
    
    protected void setUp() throws Exception {
        super.setUp();
    }

    public final void testCartridgeValidation() throws Exception {
        OMElement elt = AxiomXpathParserUtil.parse(new File(dir+"cartridges-1.xml"));
        
        // schema 1 - cartridges 
        AxiomXpathParserUtil.validate(elt, xmlSchemaCartridges);
        
        elt = AxiomXpathParserUtil.parse(new File(dir+"cartridges-5.xml"));
        AxiomXpathParserUtil.validate(elt, xmlSchemaCartridges);
        
        elt = AxiomXpathParserUtil.parse(new File(dir+"cartridges-6.xml"));
        AxiomXpathParserUtil.validate(elt, xmlSchemaCartridges);
        
        elt = AxiomXpathParserUtil.parse(new File(dir+"cartridges-7.xml"));
        AxiomXpathParserUtil.validate(elt, xmlSchemaCartridges);
        
        elt = AxiomXpathParserUtil.parse(new File(dir+"cartridges-9.xml"));
        AxiomXpathParserUtil.validate(elt, xmlSchemaCartridges);
        
        elt = AxiomXpathParserUtil.parse(new File(dir+"cartridges-2.xml"));
        // schema 2 - cartridge
        AxiomXpathParserUtil.validate(elt, xmlSchemaCartridge);
        
        elt = AxiomXpathParserUtil.parse(new File(dir+"cartridges-8.xml"));
        AxiomXpathParserUtil.validate(elt, xmlSchemaCartridge);
    }
    
    public final void testCartridgeInvalidation() {
        OMElement elt1 = AxiomXpathParserUtil.parse(new File(dir+"cartridges-1.xml"));
        OMElement elt2 = AxiomXpathParserUtil.parse(new File(dir+"cartridges-2.xml"));
        OMElement elt3 = AxiomXpathParserUtil.parse(new File(dir+"cartridges-3.xml"));
        OMElement elt4 = AxiomXpathParserUtil.parse(new File(dir+"cartridges-4.xml"));
        
     // schema 1 - cartridges 
        try {
            AxiomXpathParserUtil.validate(elt2, xmlSchemaCartridges);
        } catch (Exception e) {
            assertEquals(SAXParseException.class, e.getClass());
        }
        
        try {
            AxiomXpathParserUtil.validate(elt3, xmlSchemaCartridges);
        } catch (Exception e) {

            assertEquals(SAXParseException.class, e.getClass());
        }
        
        try {
            AxiomXpathParserUtil.validate(elt4, xmlSchemaCartridges);
        } catch (Exception e) {

            assertEquals(SAXParseException.class, e.getClass());
        }
        
        // schema 2 - cartridge
        
        try {
            AxiomXpathParserUtil.validate(elt1, xmlSchemaCartridge);
        } catch (Exception e) {

            assertEquals(SAXParseException.class, e.getClass());
        }
        
        try {
            AxiomXpathParserUtil.validate(elt3, xmlSchemaCartridge);
        } catch (Exception e) {

            assertEquals(SAXParseException.class, e.getClass());
        }
        
        try {
            AxiomXpathParserUtil.validate(elt4, xmlSchemaCartridge);
        } catch (Exception e) {

            assertEquals(SAXParseException.class, e.getClass());
        }
    }
}
