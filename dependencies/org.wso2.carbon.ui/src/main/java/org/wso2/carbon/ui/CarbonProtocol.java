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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.framework.BundleContext;
import org.osgi.service.url.AbstractURLStreamHandlerService;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;

/**
 *
 */
public class CarbonProtocol extends AbstractURLStreamHandlerService {

    private BundleContext context;

    private static final Log log = LogFactory.getLog(CarbonConnection.class);

    public CarbonProtocol(BundleContext context) {
        this.context = context;
    }

    public URLConnection openConnection(URL url) throws IOException {
        try {
            return new CarbonConnection(url, context);
        } catch (Exception e) {
            String msg = "Can't create CarbonConnection. Required Services are not available.";
            log.error(msg, e);
        }
        return null;
    }
}