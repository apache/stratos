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
package org.wso2.carbon.ui.util;

import java.net.URL;
import java.util.Set;

/**
 * Defines a set of methos to load UI resources and resource paths from resource providers such as OSGi Bundles,
 * registry, file system, etc.
 */
public interface UIResourceProvider {
    
    /**
     * Returns a URL to the resource that is mapped to a specified path. The path must begin with a "/" and is
     * interpreted as relative to the current context root.
     *
     * This method returns null  if no resource is mapped to the pathname.
     *
     * @param path a String specifying the path to the resource
     * @return the resource located at the named path, or null if there is no resource at that path
     */
    URL getUIResource(String path);

    /**
     * Returns a directory-like listing of all the paths to resources within the web application whose longest sub-path
     * matches the supplied path argument. Paths indicating subdirectory paths end with a '/'. The returned paths are
     * all relative to the root of resource provider and have a leading '/'. For example, for a resource provider
     * containing
     *
     * /welcome.html
     * /WEB_INF
     * /WEB-INF/web.xml
     * /WEB-INF/tiles
     * /WEB-INF/tiles/main_defs.xml
     *
     * getResourcePaths("/") returns {"/welcome.html", "/WEB_INF"}.
     * getResourcePaths("/WEB_INF/") returns {"/WEB-INF/web.xml", "/WEB-INF/tiles/"}.
     * getResourcePaths("/WEB-INF/tiles/") returns {"/WEB-INF/tiles/main_defs.xml"}.
     * 
     * @param path partial path used to match the resources, which must start with a /
     * @return a Set containing the directory listing, or null if there are no resources whose path begins with the
     *          supplied path.
     */
    Set<String> getUIResourcePaths(String path);
}
