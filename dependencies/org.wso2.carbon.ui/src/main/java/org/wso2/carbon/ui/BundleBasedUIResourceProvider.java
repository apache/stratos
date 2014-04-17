/*
 * Copyright 2009-2010 WSO2, Inc. (http://wso2.com)
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
package org.wso2.carbon.ui;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.framework.Bundle;
import org.wso2.carbon.ui.util.UIResourceProvider;

import java.net.URL;
import java.util.*;

public class BundleBasedUIResourceProvider implements UIResourceProvider {

    protected static final Log log = LogFactory.getLog(BundleBasedUIResourceProvider.class); 
    private Map<String, Bundle> bundleResourceMap; // resourcePath -> Bundle
    private Map<Bundle, List<String>> inverseBundleResourceMap; //  Bundle ->  resoucePath1, reosourcePath2, ...
    private String bundleResourcePath;

    public BundleBasedUIResourceProvider(String bundleResourcePath) {
        this.bundleResourcePath = bundleResourcePath;
        this.bundleResourceMap = new HashMap<String, Bundle>();
        this.inverseBundleResourceMap = new HashMap<Bundle, List<String>>();

    }

    public URL getUIResource(String name) {
        String resourceName = bundleResourcePath + name;
        int lastSlash = resourceName.lastIndexOf('/');
        if (lastSlash == -1) {
            return null;
        }

        String path = resourceName.substring(0, lastSlash);
        if (path.length() == 0) {
            path = "/";
        }
        String file = resourceName.substring(lastSlash + 1);

        //Searching the resourceBundle for the given bundle resource paths.
        String resourcePath = CarbonUIUtil.getBundleResourcePath(name);
        Bundle resourceBundle = bundleResourceMap.get(resourcePath);
        if (resourceBundle != null) {
            Enumeration entryPaths = resourceBundle.findEntries(path, file, false);
            /* Enumeration entryPaths = null;
 	try { 
 	     entryPaths = resourceBundle.getResources(path + File.separator + file); 
 	} catch (IOException ignored) { 
 	     log.error(ignored.getMessage(), ignored); 
 	}*/
            if (entryPaths != null && entryPaths.hasMoreElements()) {
                return (URL) entryPaths.nextElement();
            }
        }
        return null;
    }

    public Set<String> getUIResourcePaths(String name) {
        Set<String> result = new HashSet<String>();
        //Searching the resourceBundle for the given bundle resource paths.
        String resourcePath = CarbonUIUtil.getBundleResourcePath(name);
        Bundle resourceBundle = bundleResourceMap.get(resourcePath);

        if (resourceBundle != null) {
            Enumeration e = resourceBundle.findEntries(bundleResourcePath + name, null, false);
            if (e != null) {
                while (e.hasMoreElements()) {
                    URL entryURL = (URL) e.nextElement();
                    result.add(entryURL.getFile().substring(bundleResourcePath.length()));
                }
            }
        }
        return result;
    }

    public void addBundleResourcePaths(Bundle bundle) {
        List<String> resourcePathList = new LinkedList<String>();
        Enumeration entries = bundle.findEntries("web", "*", false);
        while (entries != null && entries.hasMoreElements()) {
            URL url = (URL) entries.nextElement();
            String path = url.getPath();
            if (path.endsWith("/")) {
                String bundleResourcePath = path.substring("/web/".length(), path.length() - 1);
                bundleResourceMap.put(bundleResourcePath, bundle);
                resourcePathList.add(bundleResourcePath);
            }
        }

        inverseBundleResourceMap.put(bundle,resourcePathList);
    }


    /*
    removing the resource paths mapped to a bundle. For this we make use of the bunde - > resourcePath
    multiple entry hashmap
     */
    public void removeBundleResourcePaths(Bundle bundle){
        List<String> resourcePathList = inverseBundleResourceMap.get(bundle);
        for(String str : resourcePathList){
            if(bundleResourceMap.containsKey(str)){
                System.out.println("Removing the resource Path : "+ str);
                bundleResourceMap.remove(str);
            }
        }

    }

    public Bundle getBundle(String resourcePath) {
        return bundleResourceMap.get(resourcePath);
    }
}
