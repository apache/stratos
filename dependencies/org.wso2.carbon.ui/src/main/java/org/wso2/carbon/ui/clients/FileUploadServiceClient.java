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
import org.apache.axis2.Constants;
import org.apache.axis2.client.Options;
import org.apache.axis2.client.ServiceClient;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.transport.http.HTTPConstants;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.core.common.IFileUpload;
import org.wso2.carbon.core.common.UploadedFileItem;
import org.wso2.carbon.core.commons.stub.fileupload.FileUploadServiceStub;

/**
 *
 */
public class FileUploadServiceClient implements IFileUpload {
    private static final Log log = LogFactory.getLog(FileUploadServiceClient.class);
    private FileUploadServiceStub stub;

    public FileUploadServiceClient(ConfigurationContext ctx,
                                   String serverURL,
                                   String cookie) throws AxisFault {
        String serviceEPR = serverURL + "FileUploadService";
        stub = new FileUploadServiceStub(ctx, serviceEPR);
        ServiceClient client = stub._getServiceClient();
        Options options = client.getOptions();
        options.setManageSession(true);
        if (cookie != null) {
            options.setProperty(HTTPConstants.COOKIE_STRING, cookie);
        }
        options.setProperty(Constants.Configuration.ENABLE_MTOM, Constants.VALUE_TRUE);
    }

    public String[] uploadFiles(UploadedFileItem[] uploadedFileItems) throws Exception {
        org.wso2.carbon.core.commons.stub.fileupload.UploadedFileItem[] newItems =
                new org.wso2.carbon.core.commons.stub.fileupload.UploadedFileItem[uploadedFileItems.length];
        int i = 0;
        for (UploadedFileItem item : uploadedFileItems) {
            org.wso2.carbon.core.commons.stub.fileupload.UploadedFileItem newItem =
                    new org.wso2.carbon.core.commons.stub.fileupload.UploadedFileItem();
            newItem.setDataHandler(item.getDataHandler());
            newItem.setFileName(item.getFileName());
            newItem.setFileType(item.getFileType());
            newItems[i++] = newItem;
        }

        return stub.uploadFiles(newItems);
    }
}
