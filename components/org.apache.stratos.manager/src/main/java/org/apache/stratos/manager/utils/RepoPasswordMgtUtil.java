/*
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 *
 */
package org.apache.stratos.manager.utils;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class RepoPasswordMgtUtil {

	private static final Log log = LogFactory.getLog(RepoPasswordMgtUtil.class);
	
	public static String getSecurityKey() {
		// TODO : a proper testing on the secure vault protected
		// user defined encryption key
		String securityKey = CartridgeConstants.DEFAULT_SECURITY_KEY;
		/*OMElement documentElement = null;
		File xmlFile = new File(CarbonUtils.getCarbonHome() + File.separator + "repository" + File.separator + "conf"
				+ File.separator + CartridgeConstants.SECURITY_KEY_FILE);

		if (xmlFile.exists()) {
			try {
				documentElement = new StAXOMBuilder(xmlFile.getPath()).getDocumentElement();
			} catch (Exception ex) {
				String msg = "Error occurred when parsing the " + xmlFile.getPath() + ".";
				log.error(msg, ex);
				ex.printStackTrace();
			}
			if (documentElement != null) {
				Iterator<?> it = documentElement.getChildrenWithName(new QName(CartridgeConstants.SECURITY_KEY));
				if (it.hasNext()) {
					OMElement securityKeyElement = (OMElement) it.next();
					SecretResolver secretResolver = SecretResolverFactory.create(documentElement, false);
					String alias = securityKeyElement.getAttributeValue(new QName(CartridgeConstants.ALIAS_NAMESPACE,
							CartridgeConstants.ALIAS_LOCALPART, CartridgeConstants.ALIAS_PREFIX));

					if (secretResolver != null && secretResolver.isInitialized()
							&& secretResolver.isTokenProtected(alias)) {
						securityKey = "";
						securityKey = secretResolver.resolve(alias);
						
					}
				}
			}
		}
        else {
            log.error(String.format("File does not exist: %s", xmlFile.getPath()));
		}*/
		return securityKey;
	}
	
	public static String encryptPassword(String repoUserPassword, String secKey) {
		String encryptPassword = "";
		String secret = secKey; // secret key length must be 16
		SecretKey key;
		Cipher cipher;
		Base64 coder;
		key = new SecretKeySpec(secret.getBytes(), "AES");
		try {
			cipher = Cipher.getInstance("AES/ECB/PKCS5Padding", "SunJCE");
			coder = new Base64();
			cipher.init(Cipher.ENCRYPT_MODE, key);
			byte[] cipherText = cipher.doFinal(repoUserPassword.getBytes());
			encryptPassword = new String(coder.encode(cipherText));
		} catch (Exception e) {
			e.printStackTrace();
		}
		return encryptPassword;
	}
	
	public static String encryptPassword(String repoUserPassword) {
		return encryptPassword(repoUserPassword,getSecurityKey());
	}

	public static String decryptPassword(String repoUserPassword, String secKey) {
		
		String decryptPassword = "";
		String secret = secKey; // secret key length must be 16
		SecretKey key;
		Cipher cipher;
		Base64 coder;
		key = new SecretKeySpec(secret.getBytes(), "AES");
		try {
			cipher = Cipher.getInstance("AES/ECB/PKCS5Padding", "SunJCE");
			coder = new Base64();
			byte[] encrypted = coder.decode(repoUserPassword.getBytes());
			cipher.init(Cipher.DECRYPT_MODE, key);
			byte[] decrypted = cipher.doFinal(encrypted);
			decryptPassword = new String(decrypted);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return decryptPassword;
	}
	
	public static String decryptPassword(String repoUserPassword) {		
		return decryptPassword(repoUserPassword,getSecurityKey());
	}
}
