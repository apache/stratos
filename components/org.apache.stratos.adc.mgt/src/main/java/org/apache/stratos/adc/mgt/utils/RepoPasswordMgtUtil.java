/**
 * 
 */
package org.apache.stratos.adc.mgt.utils;

import java.io.File;
import java.util.Iterator;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import javax.xml.namespace.QName;

import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.impl.builder.StAXOMBuilder;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.utils.CarbonUtils;
import org.wso2.securevault.SecretResolver;
import org.wso2.securevault.SecretResolverFactory;

/**
 * @author wso2
 *
 */
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
	
	public static String encryptPassword(String repoUserPassword) {
		String encryptPassword = "";
		String secret = getSecurityKey(); // secret key length must be 16
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

	public static String decryptPassword(String repoUserPassword) {
		
		String decryptPassword = "";
		String secret = getSecurityKey(); // secret key length must be 16
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
}
