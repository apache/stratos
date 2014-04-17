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

import java.net.MalformedURLException;
import java.net.URL;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.equinox.http.helper.BundleEntryHttpContext;
import org.osgi.framework.Bundle;
import org.wso2.carbon.registry.core.Registry;

public class SecuredComponentEntryHttpContext extends BundleEntryHttpContext {

	private String bundlePath;

	protected Registry registry;

    protected UIResourceRegistry uiResourceRegistry;

    protected Log log = LogFactory.getLog(this.getClass());

	public SecuredComponentEntryHttpContext(Bundle bundle) {
		super(bundle);
	}

	public SecuredComponentEntryHttpContext(Bundle bundle, String bundlePath, UIResourceRegistry uiResourceRegistry) {
		super(bundle, bundlePath);
		this.bundlePath = bundlePath;
        this.uiResourceRegistry = uiResourceRegistry;
    }

	public SecuredComponentEntryHttpContext(Bundle bundle, String s, Registry registry) {
		super(bundle, s);
		this.registry = registry;
    }

    public URL getResource(String resourceName) {
        URL url = super.getResource(resourceName);
        if (url != null) {
            return url;
        }

        url = uiResourceRegistry.getUIResource(resourceName);

        if (url != null) {
            return url;
        } else {
            int lastSlash = resourceName.lastIndexOf('/');
            if (lastSlash == -1) {
                return null;
            }
            String file = resourceName.substring(lastSlash + 1);
            if (file.endsWith(".js") && isListed(file)) {
                try {
                    return new URL("carbon://generate/" + file);
                } catch (MalformedURLException e) {
                    return null;
                }
            }
            return null;
        }
    }

   

	/**
	 * TODO: fix this from a configuration file
	 *
	 * @param file
	 *            File
	 * @return bool
	 */
	private boolean isListed(String file) {
		return "global-params.js".equals(file);
	}
}
