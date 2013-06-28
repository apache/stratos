/**
 * 
 */
package org.wso2.carbon.adc.mgt.reposync.service;

import java.util.Map;

import javax.ws.rs.Consumes;
import javax.ws.rs.FormParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.MediaType;

import net.sf.json.JSONObject;

/**
 * @author wso2
 * 
 */
@Path("/reposyncservice/")
public class RepositorySynchronizer {

	@POST
	@Path("/notify/")
	@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
	public void execute(@FormParam("payload") String payload) {
		Map jsonObject = JSONObject.fromObject(payload);
		System.out.println("Printing......");
		Map repoMap = (Map) jsonObject.get("repository");
		System.out.println("-------------");
		System.out.println("Repo URL : " + repoMap.get("url"));
		System.out.println("-------------");
		System.out.println("-------------");
		System.out.println("-------------");
		System.out.println("---JSON customer : " + payload);
	}
}
