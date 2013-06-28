 /*
  * Copyright WSO2, Inc. (http://wso2.com)
  *
  * Licensed under the Apache License, Version 2.0 (the "License");
  * you may not use this file except in compliance with the License.
  * You may obtain a copy of the License at
  *
  * http://www.apache.org/licenses/LICENSE-2.0
  *
  * Unless required by applicable law or agreed to in writing, software
  * distributed under the License is distributed on an "AS IS" BASIS,
  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  * See the License for the specific language governing permissions and
  * limitations under the License.
  */

package org.wso2.carbon.adc.mgt.dns;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.adc.mgt.utils.CartridgeConstants;

/**
 * This class is for handling dns entries.
 */
public class DNSManager {
	private static final Log log = LogFactory.getLog(DNSManager.class);

    /**
   	 * This is get called when there is a need of adding new sub domain to
   	 * exciting domain.
   	 * It will append required text to bind9 zone file and reload bind9 server.
   	 * Note: make sure the user who run ADC has got rights to run sudo scripts
   	 * without password
   	 *
   	 * @param subDomain
   	 *            will be added in front of domain
   	 */
   	public void addNewSubDomain(String subDomain, String ip) {
   		try {
   			Runtime.getRuntime()
   			       .exec(CartridgeConstants.SUDO_SH + " " +
             // script script file that will be used to edit
             // required files
             System.getProperty(CartridgeConstants.APPEND_SCRIPT) + " " +
             subDomain + " " +
             // machineIp ip of the machine DNS bind service
             // is running
             ip + " " +
             // bindFile the file which we edit to append
             // the DNS entry
             System.getProperty(CartridgeConstants.BIND_FILE_PATH));
   			log.info("New sub domain is added to zone file");
   		} catch (Exception e) {
   			log.error(e.getMessage());
   			throw new RuntimeException(e);
   		}
   	}
    /**
   	 * This is get called when there is a need of remove a sub domain.
   	 * It will remove required text from bind9 zone file and reload bind9 server.
   	 * Note: make sure the user who run ADC has got rights to run sudo scripts
   	 * without password
   	 *
   	 * @param subDomain
   	 *            will be used to delete the entry related to this
   	 */
   	public void removeSubDomain(String subDomain) {
   		try {
   			Runtime.getRuntime()
   			       .exec(CartridgeConstants.SUDO_SH + " " +
             // script script file that will be used to edit
             // required files
             System.getProperty(CartridgeConstants.REMOVE_SCRIPT) + " " +
             subDomain + " " +
             // bindFile the file which we edit to remove
             // the DNS entry
             System.getProperty(CartridgeConstants.BIND_FILE_PATH));
   			log.info("Sub domain is removed from zone file");
   		} catch (Exception e) {
   			log.error(e.getMessage());
   			throw new RuntimeException(e);
   		}
   	}

}
