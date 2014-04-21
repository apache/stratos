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
package org.wso2.carbon.ui.clients;

import org.apache.axis2.AxisFault;
import org.apache.axis2.client.Options;
import org.apache.axis2.client.ServiceClient;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.transport.http.HTTPConstants;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.core.common.IFileDownload;
import org.wso2.carbon.core.commons.stub.filedownload.FileDownloadServiceStub;

import javax.activation.DataHandler;
import javax.servlet.http.HttpSession;
import java.rmi.RemoteException;

/**
 *
 */
public class FileDownloadServiceClient implements IFileDownload {
    
    private static final Log log = LogFactory.getLog(FileDownloadServiceClient.class);
    private FileDownloadServiceStub stub;
    private HttpSession session;

    public FileDownloadServiceClient(ConfigurationContext ctx, String serverURL,
                             String cookie, HttpSession session) throws AxisFault {
        this.session = session;
        String serviceEPR = serverURL + "FileDownloadService";
        stub = new FileDownloadServiceStub(ctx, serviceEPR);
        ServiceClient client = stub._getServiceClient();
        Options options = client.getOptions();
        options.setManageSession(true);
        if (cookie != null) {
            options.setProperty(HTTPConstants.COOKIE_STRING, cookie);
        }
    }
    
    public DataHandler downloadFile(String id) {
        try {
            return stub.downloadFile(id);
        } catch (RemoteException e) {
            log.error("File download failed. ID: " + id);
        }
        return null;
    }
}
