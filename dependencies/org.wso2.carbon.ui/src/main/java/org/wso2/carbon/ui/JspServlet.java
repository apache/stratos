/*
 * Copyright 2004,2005 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.wso2.carbon.ui;

import org.osgi.framework.Bundle;

import javax.servlet.*;
import javax.servlet.descriptor.JspConfigDescriptor;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.security.Permission;
import java.security.PermissionCollection;
import java.util.*;

/**
 * <p>
 * JSPServlet wraps the Apache Jasper Servlet making it appropriate for running in an OSGi environment under the Http Service.
 * The Jasper JSPServlet makes use of the Thread Context Classloader to support compile and runtime of JSPs and to accommodate running
 * in an OSGi environment, a Bundle is used to provide the similar context normally provided by the webapp.
 * </p>
 * <p>
 * The Jasper Servlet will search the ServletContext to find JSPs, tag library descriptors, and additional information in the web.xml
 * as per the JSP 2.0 specification. In addition to the ServletContext this implementation will search the bundle (but not attached
 * fragments) for matching resources in a manner consistent with the Http Service's notion of a resource. By using alias and bundleResourcePath the JSP lookup should be in
 * line with the resource mapping specified in {102.4} of the OSGi HttpService.
 * </p>
 * <p>
 * TLD discovery is slightly different, to clarify it occurs in one of three ways:
 * <ol>
 * <li> declarations found in /WEB-INF/web.xml (found either on the bundleResourcePath in the bundle or in the ServletContext)</li>
 * <li> tld files found under /WEB-INF (found either on the bundleResourcePath in the bundle or in the ServletContext)</li>
 * <li> tld files found in jars on the Bundle-Classpath (see org.eclipse.equinox.internal.jsp.jasper.JSPClassLoader)</li>
 * </ol>
 * </p>
 * <p>
 * Other than the setting and resetting of the thread context classloader and additional resource lookups in the bundle the JSPServlet
 * is behaviourally consistent with the JSP 2.0 specification and regular Jasper operation.
 * </p>
 */
public class JspServlet extends HttpServlet {

    private static class BundlePermissionCollection extends PermissionCollection {
        private Bundle bundle;

        public BundlePermissionCollection(Bundle bundle) {
            this.bundle = bundle;
            super.setReadOnly();
        }

        public void add(Permission permission) {
             throw new SecurityException();
        }

        public boolean implies(Permission permission) {
              return bundle.hasPermission(permission);
        }

        public Enumeration elements() {
             return Collections.enumeration(Collections.EMPTY_LIST);
        }
    }
    
    private Servlet jspServlet = new org.apache.jasper.servlet.JspServlet();
    Bundle bundle;
    private BundlePermissionCollection bundlePermissions;
    private URLClassLoader jspLoader;
    private String bundleResourcePath;
    private String alias;
    private UIResourceRegistry uiResourceRegistry;

    public JspServlet(Bundle bundle, UIResourceRegistry uiResourceRegistry, String alias) {
        this.bundle = bundle;
        this.uiResourceRegistry = uiResourceRegistry;
        this.alias = (alias == null || alias.equals("/")) ? null : alias; //$NON-NLS-1$
        try {
            if (System.getSecurityManager() != null) {
                bundlePermissions = new BundlePermissionCollection(bundle);
            }
            jspLoader =  new JspClassLoader(bundle, bundlePermissions);
        } catch (Throwable e) {
            e.printStackTrace();  
        }
    }

    public JspServlet(Bundle bundle, UIResourceRegistry uiResourceRegistry) {
        this(bundle, uiResourceRegistry, null);
    }

    public void init(ServletConfig config) throws ServletException {
        ClassLoader original = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(jspLoader);
            jspServlet.init(new ServletConfigAdaptor(config));
            // If a SecurityManager is set we need to override the permissions collection set in Jasper's JSPRuntimeContext
            			if (System.getSecurityManager() != null) {
            				try {
            					Field jspRuntimeContextField = jspServlet.getClass().getDeclaredField("rctxt"); //$NON-NLS-1$
            					jspRuntimeContextField.setAccessible(true);
            					Object jspRuntimeContext = jspRuntimeContextField.get(jspServlet);
            					Field permissionCollectionField = jspRuntimeContext.getClass().getDeclaredField("permissionCollection"); //$NON-NLS-1$
            					permissionCollectionField.setAccessible(true);
            					permissionCollectionField.set(jspRuntimeContext, bundlePermissions);
            				} catch (Exception e) {
            					throw new ServletException("Cannot initialize JSPServlet. Failed to set JSPRuntimeContext permission collection."); //$NON-NLS-1$
            				}
            			}
        } finally {
            Thread.currentThread().setContextClassLoader(original);
        }
    }

    public void destroy() {
        ClassLoader original = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(jspLoader);
            jspServlet.destroy();
        } finally {
            Thread.currentThread().setContextClassLoader(original);
        }
    }

    public void service(HttpServletRequest request, HttpServletResponse response)
            throws ServletException,
                   IOException {
        String pathInfo = request.getPathInfo();
        if (pathInfo != null && pathInfo.startsWith("/WEB-INF/")) { //$NON-NLS-1$
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        ClassLoader original = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(jspLoader);
            jspServlet.service(request, response);
        } finally {
            Thread.currentThread().setContextClassLoader(original);
        }
    }

    public ServletConfig getServletConfig() {
        return jspServlet.getServletConfig();
    }

    public String getServletInfo() {
        return jspServlet.getServletInfo();
    }

    public class ServletConfigAdaptor implements ServletConfig {
        private ServletConfig config;
        private ServletContext context;

        public ServletConfigAdaptor(ServletConfig config) {
            this.config = config;
            this.context = new ServletContextAdaptor(config.getServletContext());
        }

        public String getInitParameter(String arg0) {
            return config.getInitParameter(arg0);
        }

        public Enumeration getInitParameterNames() {
            return config.getInitParameterNames();
        }

        public ServletContext getServletContext() {
            return context;
        }

        public String getServletName() {
            return config.getServletName();
        }
    }

    public class ServletContextAdaptor implements ServletContext {
        private ServletContext delegate;

        public ServletContextAdaptor(ServletContext delegate) {
            this.delegate = delegate;
        }

        public URL getResource(String name) throws MalformedURLException {
            if (alias != null && name.startsWith(alias)) {
                name = name.substring(alias.length());
            }

            URL url = uiResourceRegistry.getUIResource(name);
            if (url != null) {
                return url;
            }

            return delegate.getResource(name);
        }

        public InputStream getResourceAsStream(String name) {
            try {
                URL resourceURL = getResource(name);
                if (resourceURL != null) {
                    return resourceURL.openStream();
                }
            } catch (IOException e) {
                log("Error opening stream for resource '" + name + "'",
                    e); //$NON-NLS-1$ //$NON-NLS-2$
            }
            return null;
        }

        public Set getResourcePaths(String name) {
            Set<String> result = delegate.getResourcePaths(name);
            Set<String> resultFromProviders = uiResourceRegistry.getUIResourcePaths(name);

            //Merging two sets.
            if (resultFromProviders != null && result != null) {
                for (String resourcePath : resultFromProviders) {
                    result.add(resourcePath);
                }
                return result;
            } else if (resultFromProviders != null) {
                return resultFromProviders;
            } else {
                return result;
            }

        }

        public RequestDispatcher getRequestDispatcher(String arg0) {
            return delegate.getRequestDispatcher(arg0);
        }

        public Object getAttribute(String arg0) {
            return delegate.getAttribute(arg0);
        }

        public Enumeration getAttributeNames() {
            return delegate.getAttributeNames();
        }

        public ServletContext getContext(String arg0) {
            return delegate.getContext(arg0);
        }

        public String getInitParameter(String arg0) {
            return delegate.getInitParameter(arg0);
        }

        public Enumeration getInitParameterNames() {
            return delegate.getInitParameterNames();
        }

	public boolean setInitParameter(String s, String s1) {
            return delegate.setInitParameter(s,s1);
        }

        public int getMajorVersion() {
            return delegate.getMajorVersion();
        }

        public String getMimeType(String arg0) {
            return delegate.getMimeType(arg0);
        }

        public int getMinorVersion() {
            return delegate.getMinorVersion();
        }

	 public int getEffectiveMajorVersion() {
            return delegate.getEffectiveMajorVersion();
        }

        public int getEffectiveMinorVersion() {
            return delegate.getEffectiveMinorVersion();
        }

        public RequestDispatcher getNamedDispatcher(String arg0) {
            return delegate.getNamedDispatcher(arg0);
        }

        public String getRealPath(String arg0) {
            return delegate.getRealPath(arg0);
        }

        public String getServerInfo() {
            return delegate.getServerInfo();
        }

        /**
         * @deprecated *
         */
        public Servlet getServlet(String arg0) throws ServletException {
            return delegate.getServlet(arg0);
        }

        public String getServletContextName() {
            return delegate.getServletContextName();
        }

	public ServletRegistration.Dynamic addServlet(String s, String s1) {
            return delegate.addServlet(s, s1);
        }

        public ServletRegistration.Dynamic addServlet(String s, Servlet servlet) {
            return delegate.addServlet(s, servlet);
        }

        public ServletRegistration.Dynamic addServlet(String s, Class<? extends Servlet> aClass) {
            return delegate.addServlet(s, aClass);
        }

        public <T extends Servlet> T createServlet(Class<T> tClass) throws ServletException {
            return delegate.createServlet(tClass);
        }

        public ServletRegistration getServletRegistration(String s) {
            return delegate.getServletRegistration(s);
        }

        public Map<String, ? extends ServletRegistration> getServletRegistrations() {
            return delegate.getServletRegistrations();
        }

        public FilterRegistration.Dynamic addFilter(String s, String s1) {
            return delegate.addFilter(s,s1);
        }

        public FilterRegistration.Dynamic addFilter(String s, Filter filter) {
            return delegate.addFilter(s, filter);
        }

        public FilterRegistration.Dynamic addFilter(String s, Class<? extends Filter> aClass) {
            return delegate.addFilter(s, aClass);
        }

        public <T extends Filter> T createFilter(Class<T> tClass) throws ServletException {
            return delegate.createFilter(tClass);
        }

        public FilterRegistration getFilterRegistration(String s) {
            return delegate.getFilterRegistration(s);
        }

        public Map<String, ? extends FilterRegistration> getFilterRegistrations() {
            return delegate.getFilterRegistrations();
        }

        public SessionCookieConfig getSessionCookieConfig() {
            return delegate.getSessionCookieConfig();
        }

        public void setSessionTrackingModes(Set<SessionTrackingMode> sessionTrackingModes) throws IllegalStateException, IllegalArgumentException {
            delegate.setSessionTrackingModes(sessionTrackingModes);
        }

        public Set<SessionTrackingMode> getDefaultSessionTrackingModes() {
            return delegate.getDefaultSessionTrackingModes();
        }

        public Set<SessionTrackingMode> getEffectiveSessionTrackingModes() {
            return delegate.getEffectiveSessionTrackingModes();
        }

        public void addListener(Class<? extends EventListener> aClass) {
            delegate.addListener(aClass);
        }

        public void addListener(String s) {
            delegate.addListener(s);
        }

        public <T extends EventListener> void addListener(T t) {
            delegate.addListener(t);
        }

        public <T extends EventListener> T createListener(Class<T> tClass) throws ServletException {
            return delegate.createListener(tClass);
        }

        public void declareRoles(String... strings) {
            delegate.declareRoles(strings);
        }

        public ClassLoader getClassLoader() {
            return delegate.getClassLoader();
        }

        public JspConfigDescriptor getJspConfigDescriptor() {
            return delegate.getJspConfigDescriptor();
        }

        /**
         * @deprecated *
         */
        public Enumeration getServletNames() {
            return delegate.getServletNames();
        }

        /**
         * @deprecated *
         */
        public Enumeration getServlets() {
            return delegate.getServlets();
        }

        /**
         * @deprecated *
         */
        public void log(Exception arg0, String arg1) {
            delegate.log(arg0, arg1);
        }

        public void log(String arg0, Throwable arg1) {
            delegate.log(arg0, arg1);
        }

        public void log(String arg0) {
            delegate.log(arg0);
        }

        public void removeAttribute(String arg0) {
            delegate.removeAttribute(arg0);
        }

        public void setAttribute(String arg0, Object arg1) {
            delegate.setAttribute(arg0, arg1);
        }

        // Added in Servlet 2.5
        public String getContextPath() {
            try {
                Method getContextPathMethod =
                        delegate.getClass().getMethod("getContextPath", null); //$NON-NLS-1$
                return (String) getContextPathMethod.invoke(delegate, null);
            } catch (Exception e) {
                // ignore
            }
            return null;
        }
    }
}
