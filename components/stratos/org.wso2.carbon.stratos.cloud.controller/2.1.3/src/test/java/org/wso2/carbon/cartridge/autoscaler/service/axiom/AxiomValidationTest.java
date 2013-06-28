/*
*  Copyright (c) 2005-2011, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
*
*  WSO2 Inc. licenses this file to you under the Apache License,
*  Version 2.0 (the "License"); you may not use this file except
*  in compliance with the License.
*  You may obtain a copy of the License at
*
*    http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing,
* software distributed under the License is distributed on an
* "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
* KIND, either express or implied.  See the License for the
* specific language governing permissions and limitations
* under the License.
*/
package org.wso2.carbon.cartridge.autoscaler.service.axiom;

import java.io.File;

import org.wso2.carbon.stratos.cloud.controller.axiom.AxiomXpathParser;
import org.xml.sax.SAXParseException;

import junit.framework.TestCase;

public class AxiomValidationTest extends TestCase {
    
    AxiomXpathParser util1, util2, util3, util4, util5, util6, util7, util8, util9;
    File xmlSchemaCartridges = new File("src/main/resources/cartridges.xsd");
    File xmlSchemaCartridge = new File("src/main/resources/cartridge.xsd");
    String dir = "src/test/resources/";

    public AxiomValidationTest(String name) {
        super(name);
    }
    
    protected void setUp() throws Exception {
        super.setUp();
        util1 = new AxiomXpathParser(new File(dir+"cartridges-1.xml"));
        util2 = new AxiomXpathParser(new File(dir+"cartridges-2.xml"));
        util3 = new AxiomXpathParser(new File(dir+"cartridges-3.xml"));
        util4 = new AxiomXpathParser(new File(dir+"cartridges-4.xml"));
        util5 = new AxiomXpathParser(new File(dir+"cartridges-5.xml"));
        util6 = new AxiomXpathParser(new File(dir+"cartridges-6.xml"));
        util7 = new AxiomXpathParser(new File(dir+"cartridges-7.xml"));
        util8 = new AxiomXpathParser(new File(dir+"cartridges-8.xml"));
        util9 = new AxiomXpathParser(new File(dir+"cartridges-9.xml"));
        util1.parse();
        util2.parse();
        util3.parse();
        util4.parse();
        util5.parse();
        util6.parse();
        util7.parse();
        util8.parse();
        util9.parse();
    }

    public final void testCartridgeValidation() throws Exception {
        
        // schema 1 - cartridges 
        assertEquals(true, util1.validate(xmlSchemaCartridges));
        
        assertEquals(true, util5.validate(xmlSchemaCartridges));
        
        assertEquals(true, util6.validate(xmlSchemaCartridges));
        
        assertEquals(true, util7.validate(xmlSchemaCartridges));
        
        assertEquals(true, util9.validate(xmlSchemaCartridges));
        
        // schema 2 - cartridge
        assertEquals(true, util2.validate(xmlSchemaCartridge));
        
        assertEquals(true, util8.validate(xmlSchemaCartridge));
    }
    
    public final void testCartridgeInvalidation() {
        
     // schema 1 - cartridges 
        try {
            util2.validate(xmlSchemaCartridges);
        } catch (Exception e) {
            assertEquals(SAXParseException.class, e.getClass());
        }
        
        try {
            util3.validate(xmlSchemaCartridges);
        } catch (Exception e) {

            assertEquals(SAXParseException.class, e.getClass());
        }
        
        try {
            util4.validate(xmlSchemaCartridges);
        } catch (Exception e) {

            assertEquals(SAXParseException.class, e.getClass());
        }
        
        // schema 2 - cartridge
        
        try {
            util1.validate(xmlSchemaCartridge);
        } catch (Exception e) {

            assertEquals(SAXParseException.class, e.getClass());
        }
        
        try {
            util3.validate(xmlSchemaCartridge);
        } catch (Exception e) {

            assertEquals(SAXParseException.class, e.getClass());
        }
        
        try {
            util4.validate(xmlSchemaCartridge);
        } catch (Exception e) {

            assertEquals(SAXParseException.class, e.getClass());
        }
    }
}
