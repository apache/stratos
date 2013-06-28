package org.wso2.carbon.stratos.cloud.controller.util;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import javax.xml.stream.XMLStreamException;

import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.util.AXIOMUtil;
import org.apache.commons.io.FileUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.stratos.cloud.controller.exception.CloudControllerException;

public class CloudControllerUtil {
	private static final Log log = LogFactory.getLog(CloudControllerUtil.class);

	public static OMElement serviceCtxtToOMElement(ServiceContext ctxt) throws XMLStreamException{
		String xml;
		
		xml = ctxt.toXml();
		
		return AXIOMUtil.stringToOM(xml);
	}

	public static byte[] getBytesFromFile(String path) {

		try {
	        return FileUtils.readFileToByteArray(new File(path));
        } catch (IOException e) {

        	handleException("Failed to read the file "+path, e);
        }
		return new byte[0];
    }
	
	public static CartridgeInfo toCartridgeInfo(Cartridge cartridge) {

		CartridgeInfo carInfo = new CartridgeInfo();
		carInfo.setType(cartridge.getType());
		carInfo.setDisplayName(cartridge.getDisplayName());
		carInfo.setDescription(cartridge.getDescription());
		carInfo.setHostName(cartridge.getHostName());
		carInfo.setDeploymentDirs(cartridge.getDeploymentDirs());
		carInfo.setProvider(cartridge.getProvider());
		carInfo.setVersion(cartridge.getVersion());
		carInfo.setMultiTenant(cartridge.isMultiTenant());
		carInfo.setBaseDir(cartridge.getBaseDir());
		carInfo.setPortMappings(cartridge.getPortMappings()
		                                 .toArray(new PortMapping[cartridge.getPortMappings()
		                                                                   .size()]));
		carInfo.setAppTypes(cartridge.getAppTypeMappings()
                                .toArray(new AppType[cartridge.getAppTypeMappings()
                                                                  .size()]));
		
		List<Property> propList = new ArrayList<Property>();
		
		for (Iterator<?> iterator = cartridge.getProperties().entrySet().iterator(); iterator.hasNext();) {
	        @SuppressWarnings("unchecked")
            Map.Entry<String, String> entry = (Entry<String, String>) iterator.next();
	        
	        Property prop = new Property(entry.getKey(), entry.getValue());
	        propList.add(prop);
        }
		Property[] props = new Property[propList.size()];
		
		carInfo.setProperties(propList.toArray(props));

		return carInfo;
	}
	
	public static List<Object> getKeysFromValue(Map<?, ?> hm, Object value) {
		List<Object> list = new ArrayList<Object>();
		for (Object o : hm.keySet()) {
			if (hm.get(o).equals(value)) {
				list.add(o);
			}
		}
		return list;
	}
	
	public static void sleep(long time){
    	try {
    		Thread.sleep(time);
    	} catch (InterruptedException ignore) {}
    	
    }
	
	public static void handleException(String msg, Exception e){
		log.error(msg, e);
		throw new CloudControllerException(msg, e);
	}
	
	public static void handleException(String msg){
		log.error(msg);
		throw new CloudControllerException(msg);
	}
}
