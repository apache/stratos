package org.apache.stratos.metadataservice.services;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;

import org.apache.commons.configuration.XMLConfiguration;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.metadataservice.annotation.AuthorizationAction;
import org.apache.stratos.metadataservice.definition.CartridgeMetaData;
import org.apache.stratos.metadataservice.exception.RestAPIException;
import org.apache.stratos.metadataservice.registry.DataRegistryFactory;
import org.apache.stratos.metadataservice.util.ConfUtil;

@Path("/metadataservice/")
public class MetaDataAdmin {

	private static Log log = LogFactory.getLog(MetaDataAdmin.class);
	@Context
	HttpServletRequest httpServletRequest;

	private final String defaultRegType = "GREG";

	private XMLConfiguration conf;

	@POST
	@Path("/init")
	@AuthorizationAction("/permission/protected/manage/monitor/tenants")
	public void initialize() throws RestAPIException {
		conf = ConfUtil.getInstance(null).getConfiguration();
	}

	@POST
	@Path("/cartridge/metadata/{applicationname}/{cartridgetype}")
	@Produces("application/json")
	@Consumes("application/json")
	@AuthorizationAction("/permission/protected/manage/monitor/tenants")
	public String addCartridgeMetaDataDetails(@PathParam("applicationname") String applicationName,
	                                          @PathParam("cartridgetype") String cartridgeType,
	                                          CartridgeMetaData cartridgeMetaData) throws Exception {

		conf = ConfUtil.getInstance(null).getConfiguration();

		String registryType =
		                      conf.getString("metadataservice.govenanceregistrytype",
		                                     defaultRegType);
		return DataRegistryFactory.getDataRegistryFactory(registryType)
		                          .addCartridgeMetaDataDetails(applicationName, cartridgeType,
		                                                       cartridgeMetaData);

	}

	@GET
	@Path("/cartridge/metadata/{applicationname}/{cartridgetype}")
	@Produces("application/json")
	@Consumes("application/json")
	@AuthorizationAction("/permission/protected/manage/monitor/tenants")
	public String getCartridgeMetaDataDetails(@PathParam("applicationname") String applicationName,
	                                          @PathParam("cartridgetype") String cartridgeType)

	throws Exception {
		conf = ConfUtil.getInstance(null).getConfiguration();
		String registryType =
		                      conf.getString("metadataservice.govenanceregistrytype",
		                                     defaultRegType);
		return DataRegistryFactory.getDataRegistryFactory(registryType)
		                          .getCartridgeMetaDataDetails(applicationName, cartridgeType);

	}

	public boolean removeCartridgeMetaDataDetails(String applicationName, String cartridgeType)
	                                                                                           throws Exception {
		conf = ConfUtil.getInstance(null).getConfiguration();
		String registryType =
		                      conf.getString("metadataservice.govenanceregistrytype",
		                                     defaultRegType);
		return DataRegistryFactory.getDataRegistryFactory(registryType)
		                          .removeCartridgeMetaDataDetails(applicationName, cartridgeType);

	}
}
