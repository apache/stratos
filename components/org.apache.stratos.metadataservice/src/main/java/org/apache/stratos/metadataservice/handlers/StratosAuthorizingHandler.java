/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.stratos.metadataservice.handlers;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.cxf.common.util.ClassHelper;
import org.apache.cxf.frontend.MethodDispatcher;
import org.apache.cxf.interceptor.security.AccessDeniedException;
import org.apache.cxf.jaxrs.ext.RequestHandler;
import org.apache.cxf.jaxrs.model.ClassResourceInfo;
import org.apache.cxf.message.Message;
import org.apache.cxf.service.Service;
import org.apache.cxf.service.model.BindingOperationInfo;
import org.apache.stratos.metadataservice.Utils;
import org.apache.stratos.metadataservice.context.AuthenticationContext;
import org.wso2.carbon.context.CarbonContext;
import org.wso2.carbon.context.PrivilegedCarbonContext;
import org.wso2.carbon.user.api.AuthorizationManager;
import org.wso2.carbon.user.api.UserRealm;
import org.wso2.carbon.user.api.UserStoreException;
import org.wso2.carbon.utils.multitenancy.MultitenantConstants;

/**
 * {@link StratosAuthorizingHandler} authorize resource requests. It collects
 * expected permission
 * details using annotations present in the service bean. This particular
 * implementation is inspired
 * by the {@link org.apache.cxf.jaxrs.security.SimpleAuthorizingFilter}
 */
public class StratosAuthorizingHandler implements RequestHandler {
	private final Log log = LogFactory.getLog(StratosAuthorizingHandler.class);

	private static String SUPPORTED_AUTHENTICATION_TYPE = "Basic";
	private static final String AUTHORIZATION_ANNOTATION_CLASS_NAME =
	                                                                  "org.apache.stratos.metadataservice.annotation.AuthorizationAction";
	private static final String TENANT_ANNOTATION_CLASS_NAME =
	                                                           "org.apache.stratos.metadataservice.annotation.SuperTenantService";
	private static final String ACTION_ON_RESOURCE = "ui.execute";
	private static final Set<String> SKIP_METHODS;
	private Map<String, String> authorizationActionMap = Collections.emptyMap();
	private Set<String> superTenantServiceSet = Collections.emptySet();

	static {
		SKIP_METHODS = new HashSet<String>();
		SKIP_METHODS.addAll(Arrays.asList(new String[] { "wait", "notify", "notifyAll", "equals",
		                                                "toString", "hashCode" }));
	}

	@Override
	public Response handleRequest(Message message, ClassResourceInfo resourceClass) {
		try {
			AuthenticationContext.setAuthenticated(false); // TODO : fix this
			                                               // properly
			String userName = CarbonContext.getThreadLocalCarbonContext().getUsername();
			String tenantDomain = CarbonContext.getThreadLocalCarbonContext().getTenantDomain();
			int tenantId = CarbonContext.getThreadLocalCarbonContext().getTenantId();
			if (log.isDebugEnabled()) {
				log.debug("authorizing the action using" +
				          StratosAuthorizingHandler.class.getName());
				log.debug("username :" + userName);
				log.debug("tenantDomain" + tenantDomain);
				log.debug("tenantId :" + tenantId);
			}
			Method targetMethod = getTargetMethod(message);
			if (!authorize(userName, tenantDomain, tenantId, targetMethod)) {
				log.warn("User :" + userName + "trying to perform unauthrorized action" +
				         " against the resource :" + targetMethod);
				return Response.status(Response.Status.FORBIDDEN)
				               .type(MediaType.APPLICATION_JSON)
				               .entity(Utils.buildMessage("The user does not have required permissions to "
				                                          + "perform this operation")).build();
			}
			return null;

		} catch (Exception exception) {
			log.error("Unexpected error occured while REST api, authorization process", exception);
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
			               .type(MediaType.APPLICATION_JSON)
			               .entity(Utils.buildMessage("Unexpected error. Please contact the system admin"))
			               .build();
		}
	}

	private boolean authorize(String userName, String tenantDomain, int tenantId,
	                          Method targetMethod) throws Exception {
		// first we try to see whether this is a super.tenant only operation
		if (superTenantServiceSet.contains(targetMethod.getName()) &&
		    !isCurrentUserSuperTenant(tenantDomain, tenantId)) {
			return false;
		}
		// authorize using permissionString given as annotation in the service
		// class
		String permissionString = authorizationActionMap.get(targetMethod.getName());

		// get the authorization manager for this tenant..
		UserRealm userRealm = PrivilegedCarbonContext.getThreadLocalCarbonContext().getUserRealm();
		AuthorizationManager authorizationManager = userRealm.getAuthorizationManager();

		boolean isAuthorized =
		                       isAuthorized(authorizationManager, userName, permissionString,
		                                    ACTION_ON_RESOURCE);
		return isAuthorized;

	}

	private boolean isCurrentUserSuperTenant(String tenantDomain, int tenantId) {
		if (MultitenantConstants.SUPER_TENANT_DOMAIN_NAME.equals(tenantDomain) &&
		    MultitenantConstants.SUPER_TENANT_ID == tenantId) {
			return true;
		}
		return false;
	}

	private boolean isAuthorized(AuthorizationManager authorizationManager, String username,
	                             String permissionString, String action) throws UserStoreException {
		boolean isAuthorized = false;
		String[] resourceIds = permissionString.trim().split(",");
		for (String resourceId : resourceIds) {
			if (authorizationManager.isUserAuthorized(username, resourceId, action)) {
				isAuthorized = true;
				break;
			}
		}
		return isAuthorized;
	}

	/**
	 * Here we are getting the target invocation method. The method get set as a
	 * properties in the
	 * message by the
	 * {@link org.apache.cxf.jaxrs.interceptor.JAXRSInInterceptor}
	 * 
	 * @param message
	 *            incoming message
	 * @return
	 */
	protected Method getTargetMethod(Message message) {
		BindingOperationInfo bop = message.getExchange().get(BindingOperationInfo.class);
		if (bop != null) {
			MethodDispatcher md =
			                      (MethodDispatcher) message.getExchange().get(Service.class)
			                                                .get(MethodDispatcher.class.getName());
			return md.getMethod(bop);
		}
		Method method = (Method) message.get("org.apache.cxf.resource.method");
		if (method != null) {
			return method;
		}
		log.error("The requested resource is not found. Please check the resource path etc..");
		throw new AccessDeniedException("Method is not available : Unauthorized");
	}

	/**
	 * The instance of the secured bean get injected by the IOC framework
	 * 
	 * @param securedObject
	 */
	public void setSecuredObject(Object securedObject) {
		Class<?> clazz = ClassHelper.getRealClass(securedObject);
		authorizationActionMap = getAuthorizationActionMap(clazz);
		superTenantServiceSet = getSuperTenantServiceSet(clazz);

	}

	private Set<String> getSuperTenantServiceSet(Class<?> clazz) {
		Set<String> superTenantServiceSet = new HashSet<String>();
		findSuperTenantServices(clazz, superTenantServiceSet);
		return superTenantServiceSet;
	}

	private Map<String, String> getAuthorizationActionMap(Class<?> clazz) {
		Map<String, String> authorizationActionMap = new HashMap<String, String>();
		findAuthorizationActions(clazz, authorizationActionMap);
		return authorizationActionMap;
	}

	/**
	 * Goes through the class hierarchy and find the authorization annotations
	 * attached to a certain
	 * method.
	 * 
	 * @param clazz
	 *            class to be scanned
	 * @param authorizationActionMap
	 *            the map to be populated
	 */
	private void findAuthorizationActions(Class<?> clazz, Map<String, String> authorizationActionMap) {
		if (clazz == null || clazz == Object.class) {
			return;
		}
		String classAuthorizationActionsAllowed =
		                                          getAuthorizationActions(clazz.getAnnotations(),
		                                                                  AUTHORIZATION_ANNOTATION_CLASS_NAME);
		for (Method m : clazz.getMethods()) {
			if (SKIP_METHODS.contains(m.getName())) {
				continue;
			}
			String methodAuthorizationActionsAllowed =
			                                           getAuthorizationActions(m.getAnnotations(),
			                                                                   AUTHORIZATION_ANNOTATION_CLASS_NAME);
			String authorizationActions =
			                              methodAuthorizationActionsAllowed != null
			                                                                       ? methodAuthorizationActionsAllowed
			                                                                       : classAuthorizationActionsAllowed;
			if (authorizationActions != null) {
				authorizationActionMap.put(m.getName(), authorizationActions);
			}
		}
		if (!authorizationActionMap.isEmpty()) {
			return;
		}

		findAuthorizationActions(clazz.getSuperclass(), authorizationActionMap);

		if (!authorizationActionMap.isEmpty()) {
			return;
		}

		for (Class<?> interfaceCls : clazz.getInterfaces()) {
			findAuthorizationActions(interfaceCls, authorizationActionMap);
		}
	}

	/**
	 * Goes through the class hierarchy and figure out the supertenant
	 * annotations coupled with operations/methods.
	 * 
	 * @param clazz
	 * @param superTenantServiceSet
	 */
	private void findSuperTenantServices(Class<?> clazz, Set<String> superTenantServiceSet) {
		if (clazz == null || clazz == Object.class) {
			return;
		}
		for (Method m : clazz.getMethods()) {
			if (SKIP_METHODS.contains(m.getName())) {
				continue;
			}
			boolean isSuperTenantService =
			                               getSuperTenantServices(m.getAnnotations(),
			                                                      TENANT_ANNOTATION_CLASS_NAME);
			if (isSuperTenantService) {
				superTenantServiceSet.add(m.getName());
			}
		}
		if (!superTenantServiceSet.isEmpty()) {
			return;
		}

		findSuperTenantServices(clazz.getSuperclass(), superTenantServiceSet);

		if (!superTenantServiceSet.isEmpty()) {
			return;
		}

		for (Class<?> interfaceCls : clazz.getInterfaces()) {
			findSuperTenantServices(interfaceCls, superTenantServiceSet);
		}
	}

	private boolean getSuperTenantServices(Annotation[] annotations,
	                                       String tenantAnnotationClassName) {
		for (Annotation ann : annotations) {
			if (ann.annotationType().getName().equals(tenantAnnotationClassName)) {
				try {
					Method valueMethod = ann.annotationType().getMethod("value", new Class[] {});
					boolean isSuperTenantService =
					                               (Boolean) valueMethod.invoke(ann,
					                                                            new Object[] {});
					return isSuperTenantService;
				} catch (Exception ex) {
					// ignore
				}
				break;
			}
		}
		return false;
	}

	private String getAuthorizationActions(Annotation[] annotations,
	                                       String authorizationAnnotationClassName) {
		for (Annotation ann : annotations) {
			if (ann.annotationType().getName().equals(authorizationAnnotationClassName)) {
				try {
					Method valueMethod = ann.annotationType().getMethod("value", new Class[] {});
					String[] permissions = (String[]) valueMethod.invoke(ann, new Object[] {});
					StringBuilder sb = new StringBuilder();
					for (int i = 0; i < permissions.length; i++) {
						sb.append(permissions[i]);
						if (i + 1 < permissions.length) {
							sb.append(",");
						}
					}
					return sb.toString();
				} catch (Exception ex) {
					// ignore
				}
				break;
			}
		}
		return null;
	}

}
