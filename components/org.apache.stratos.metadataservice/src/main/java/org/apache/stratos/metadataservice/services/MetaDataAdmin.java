package org.apache.stratos.metadataservice.services;

import org.apache.commons.configuration.XMLConfiguration;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.metadataservice.annotation.AuthorizationAction;
import org.apache.stratos.metadataservice.definition.NewProperty;
import org.apache.stratos.metadataservice.exception.RestAPIException;
import org.apache.stratos.metadataservice.registry.DataRegistryFactory;
import org.apache.stratos.metadataservice.util.ConfUtil;
import org.wso2.carbon.registry.api.RegistryException;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.net.URI;
import java.util.List;

@Path("/")
public class MetaDataAdmin {
    @Context
    UriInfo uriInfo;

	private static Log log = LogFactory.getLog(MetaDataAdmin.class);
	@Context
	HttpServletRequest httpServletRequest;

	private final String defaultRegType = "carbon";

	private XMLConfiguration conf;
    private String registryType;

    public MetaDataAdmin(){
        conf = ConfUtil.getInstance(null).getConfiguration();
        registryType =  conf.getString("metadataservice.govenanceregistrytype", defaultRegType);
    }
	@POST
	@Path("/init")
	@AuthorizationAction("/permission/protected/manage/monitor/tenants")
	public void initialize() throws RestAPIException {

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

    @GET
    @Path("/application/{application_id}/cluster/{cluster_id}/properties")
    @Produces("application/json")
    @Consumes("application/json")
    @AuthorizationAction("/permission/protected/manage/monitor/tenants")
    public Response getClusterProperties(@PathParam("application_id") String applicationId, @PathParam("cluster_id") String clusterId){

        List<NewProperty> properties;
        NewProperty[] propertiesArr = null;
        try {
            properties = DataRegistryFactory.getDataRegistryFactory(registryType)
                    .getPropertiesOfCluster(applicationId, clusterId);
            if(properties != null) {
                propertiesArr = new NewProperty[properties.size()];
                propertiesArr = properties.toArray(propertiesArr);
            }
        } catch (Exception e) {
            log.error("Error occurred while getting properties ", e);
        }

        Response.ResponseBuilder rb=null;
        if(propertiesArr == null){
            rb = Response.status(Response.Status.NOT_FOUND);
        }else{
            rb = Response.ok().entity(propertiesArr);
        }
        return rb.build();
    }

    @GET
    @Path("/application/{application_id}/cluster/{cluster_id}/property/{property_name}")
    @Produces("application/json")
    @Consumes("application/json")
    @AuthorizationAction("/permission/protected/manage/monitor/tenants")
    public Response getClusterProperty(@PathParam("application_id") String applicationId, @PathParam("cluster_id") String clusterId, @PathParam("property_name") String propertyName){
        List<NewProperty> properties = null;
        NewProperty property = null;

        try {
            properties = DataRegistryFactory.getDataRegistryFactory(registryType)
                    .getPropertiesOfCluster(applicationId, clusterId);
            if(properties == null){
                return Response.status(Response.Status.NOT_FOUND).build();
            }
            for(NewProperty p : properties){
                if(propertyName.equals(p.getKey())){
                    property = p;
                    break;
                }
            }
        } catch (Exception e) {
            log.error("Error occurred while getting property ", e);
        }

        Response.ResponseBuilder rb=null;
        if(property == null){
            rb = Response.status(Response.Status.NOT_FOUND);
        }else{
            rb = Response.ok().entity(property);
        }
        return rb.build();
    }

    @GET
    @Path("/application/{application_id}/cluster/{cluster_id}/dependencies")
    @Produces("application/json")
    @Consumes("application/json")
    @AuthorizationAction("/permission/protected/manage/monitor/tenants")
    public Response getClusterDependencies(@PathParam("application_id") String applicationId, @PathParam("cluster_id") String clusterId){
        List<NewProperty> properties;
        NewProperty property = null;

        try {
            properties = DataRegistryFactory.getDataRegistryFactory(registryType)
                    .getPropertiesOfCluster(applicationId, clusterId);
            if(properties == null){
                return Response.status(Response.Status.NOT_FOUND).build();
            }
            for(NewProperty p : properties){
                if("dependencies".equals(p.getKey())){
                    property = p;
                    break;
                }
            }
        } catch (Exception e) {
            log.error("Error occurred while getting properties ", e);
        }
        Response.ResponseBuilder rb=null;
        if(property == null){
            rb = Response.status(Response.Status.NOT_FOUND);
        }else{
            rb = Response.ok().entity(property);
        }
        return rb.build();
    }

    @GET
    @Path("/application/{application_id}/properties")
    @Produces("application/json")
    @Consumes("application/json")
    @AuthorizationAction("/permission/protected/manage/monitor/tenants")
    public Response getApplicationProperties(@PathParam("application_id") String applicationId){
        List<NewProperty> properties;
        NewProperty[] propertiesArr = null;
        try {
            properties = DataRegistryFactory.getDataRegistryFactory(registryType)
                    .getPropertiesOfApplication(applicationId);
            if(properties != null) {
                propertiesArr = new NewProperty[properties.size()];
                propertiesArr = properties.toArray(propertiesArr);
            }
        } catch (Exception e) {
            log.error("Error occurred while getting properties ", e);
        }

        Response.ResponseBuilder rb=null;
        if(propertiesArr == null){
            rb = Response.status(Response.Status.NOT_FOUND);
        }else{
            rb = Response.ok().entity(propertiesArr);
        }
        return rb.build();
    }

    @GET
    @Path("/application/{application_id}/property/{property_name}")
    @Produces("application/json")
    @Consumes("application/json")
    @AuthorizationAction("/permission/protected/manage/monitor/tenants")
    public Response getApplicationProperty(@PathParam("application_id") String applicationId, @PathParam("property_name") String propertyName){
        List<NewProperty> properties;
        NewProperty property = null;

        try {
            properties = DataRegistryFactory.getDataRegistryFactory(registryType)
                    .getPropertiesOfApplication(applicationId);
            if(properties == null){
                return Response.status(Response.Status.NOT_FOUND).build();
            }
            for(NewProperty p : properties){
                if(propertyName.equals(p.getKey())){
                    property = p;
                    break;
                }
            }
        } catch (RegistryException e) {
            log.error("Error occurred while getting property", e);
        }

        Response.ResponseBuilder rb=null;
        if(property == null){
            rb = Response.status(Response.Status.NOT_FOUND);
        }else{
            rb = Response.ok().entity(property);
        }
        return rb.build();
    }


    @POST
    @Path("/application/{application_id}/cluster/{cluster_id}/dependencies")
    @Produces("application/json")
    @Consumes("application/json")
    @AuthorizationAction("/permission/protected/manage/monitor/tenants")
    public Response addClusterDependencies(@PathParam("application_id") String applicationId, @PathParam("cluster_id") String clusterId,  NewProperty property) throws RestAPIException {

        if(!property.getKey().equals("dependencies")){
            throw new RestAPIException("Property name should be dependencies");
        }
        URI url =  uriInfo.getAbsolutePathBuilder().path(applicationId + "/" + clusterId + "/" + property.getKey()).build();

        try {
            DataRegistryFactory.getDataRegistryFactory(registryType).addPropertyToCluster(applicationId, clusterId, property);
        } catch (RegistryException e) {
            log.error("Error occurred while adding dependencies ", e);
        }
        return Response.created(url).build();
    }

    @POST
    @Path("application/{application_id}/cluster/{cluster_id}/property")
    @Produces("application/json")
    @Consumes("application/json")
    @AuthorizationAction("/permission/protected/manage/monitor/tenants")
    public Response addPropertyToACluster(@PathParam("application_id") String applicationId, @PathParam("cluster_id") String clusterId, NewProperty property)
            throws RestAPIException {

        URI url =  uriInfo.getAbsolutePathBuilder().path(applicationId + "/" + clusterId + "/" + property.getKey()).build();

        try {
            DataRegistryFactory.getDataRegistryFactory(registryType).addPropertyToCluster(applicationId, clusterId, property);
        } catch (RegistryException e) {
            log.error("Error occurred while adding property", e);
        }

        return Response.created(url).build();

    }

    @POST
    @Path("application/{application_id}/cluster/{cluster_id}/properties")
    @Produces("application/json")
    @Consumes("application/json")
    @AuthorizationAction("/permission/protected/manage/monitor/tenants")
    public Response addPropertiesToACluster(@PathParam("application_id") String applicationId, @PathParam("cluster_id") String clusterId, NewProperty[] properties)
            throws RestAPIException {
        URI url =  uriInfo.getAbsolutePathBuilder().path(applicationId + "/" + clusterId).build();

        try {
            DataRegistryFactory.getDataRegistryFactory(registryType).addPropertiesToCluster(applicationId, clusterId, properties);
        } catch (Exception e) {
           log.error("Error occurred while adding properties ", e);
        }


        return Response.created(url).build();
    }

    @POST
    @Path("application/{application_id}/properties")
    @Produces("application/json")
    @Consumes("application/json")
    @AuthorizationAction("/permission/protected/manage/monitor/tenants")
    public Response addPropertiesToApplication(@PathParam("application_id") String applicationId,NewProperty[] properties)
            throws RestAPIException {
        URI url =  uriInfo.getAbsolutePathBuilder().path(applicationId).build();

        try {
            DataRegistryFactory.getDataRegistryFactory(registryType).addPropertiesToApplication(applicationId, properties);
        } catch (Exception e) {
            log.error("Error occurred while adding properties ", e);
        }


        return Response.created(url).build();
    }

    @POST
    @Path("application/{application_id}/property")
    @Produces("application/json")
    @Consumes("application/json")
    @AuthorizationAction("/permission/protected/manage/monitor/tenants")
    public Response addPropertyToApplication(@PathParam("application_id") String applicationId, NewProperty property)
            throws RestAPIException {
        URI url =  uriInfo.getAbsolutePathBuilder().path(applicationId).build();

        try {
            DataRegistryFactory.getDataRegistryFactory(registryType).addPropertyToApplication(applicationId, property);
        } catch (Exception e) {
            log.error("Error occurred while adding property ", e);
        }


        return Response.created(url).build();
    }
}
