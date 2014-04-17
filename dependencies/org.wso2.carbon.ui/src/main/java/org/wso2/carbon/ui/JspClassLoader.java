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
import org.osgi.framework.Constants;
import org.wso2.carbon.ui.internal.CarbonUIServiceComponent;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.security.CodeSource;
import java.security.PermissionCollection;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.StringTokenizer;

/**
 * Jasper requires that this class loader be an instance of URLClassLoader.
 * At runtime it uses the URLClassLoader's getURLs method to find jar files that are in turn searched for TLDs. In a webapp
 * these jar files would normally be located in WEB-INF/lib. In the OSGi context, this behaviour is provided by returning the
 * URLs of the jar files contained on the Bundle-ClassPath. Other than jar file tld resources this classloader is not used for
 * loading classes which should be done by the other contained class loaders.
 * <p/>
 * The rest of the ClassLoader is as follows:
 * 1) Thread-ContextClassLoader (top - parent) -- see ContextFinder
 * 2) Jasper Bundle
 * 3) The Bundle referenced at JSPServlet creation
 */
public class JspClassLoader extends URLClassLoader {

    private static final Bundle JASPERBUNDLE = CarbonUIServiceComponent.getBundle(JspServlet.class);
    private static final ClassLoader PARENT = JspClassLoader.class.getClassLoader().getParent();
    private static final String JAVA_PACKAGE = "java.";     //$NON-NLS-1$
    private static final ClassLoader EMPTY_CLASSLOADER = new ClassLoader(null) {
        public URL getResource(String name) {
            return null;
        }

        public Enumeration findResources(String name) throws IOException {
            return new Enumeration() {
                public boolean hasMoreElements() {
                    return false;
                }

                public Object nextElement() {
                    return null;
                }
            };
        }

        public Class loadClass(String name) throws ClassNotFoundException {
            throw new ClassNotFoundException(name);
        }
    };

     private PermissionCollection permissions;

    public JspClassLoader(Bundle bundle, PermissionCollection permissions) {
        super(new URL[0], new BundleProxyClassLoader(bundle, new BundleProxyClassLoader(
                JASPERBUNDLE, new JSPContextFinder(EMPTY_CLASSLOADER))));
        this.permissions = permissions;
        addBundleClassPathJars(bundle);
    }

    private void addBundleClassPathJars(Bundle bundle) {
        Dictionary headers = bundle.getHeaders();
        String classPath = (String) headers.get(Constants.BUNDLE_CLASSPATH);
        if (classPath != null) {
            StringTokenizer tokenizer = new StringTokenizer(classPath, ","); //$NON-NLS-1$
            while (tokenizer.hasMoreTokens()) {
                String candidate = tokenizer.nextToken().trim();
                if (candidate.endsWith(".jar")) { //$NON-NLS-1$
                    URL entry = bundle.getEntry(candidate);
                    if (entry != null) {
                        URL jarEntryURL;
                        try {
                            jarEntryURL = new URL(
                                    "jar:" + entry.toString() + "!/"); //$NON-NLS-1$ //$NON-NLS-2$
                            super.addURL(jarEntryURL);
                        } catch (MalformedURLException e) {
                            // TODO should log this.
                        }
                    }
                }
            }
        }
    }

    protected Class loadClass(String name, boolean resolve) throws ClassNotFoundException {
        if (PARENT != null && name.startsWith(JAVA_PACKAGE)) {
            return PARENT.loadClass(name);
        }
        return super.loadClass(name, resolve);
    }

    // Classes should "not" be loaded by this classloader from the URLs - it is just used for TLD resource discovery.
    protected Class findClass(String name) throws ClassNotFoundException {
        throw new ClassNotFoundException(name);
    }

    protected PermissionCollection getPermissions(CodeSource codesource) {
        return permissions;
    }
}
