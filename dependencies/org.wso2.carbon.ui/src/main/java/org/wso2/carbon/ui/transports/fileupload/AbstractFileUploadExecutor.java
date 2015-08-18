/*
 * Copyright 2005-2007 WSO2, Inc. (http://wso2.com)
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
package org.wso2.carbon.ui.transports.fileupload;

import org.apache.axis2.context.ConfigurationContext;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileItemFactory;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.fileupload.servlet.ServletRequestContext;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.CarbonConstants;
import org.wso2.carbon.CarbonException;
import org.wso2.carbon.base.ServerConfiguration;
import org.wso2.carbon.core.common.UploadedFileItem;
import org.wso2.carbon.ui.CarbonUIMessage;
import org.wso2.carbon.ui.clients.FileUploadServiceClient;
import org.wso2.carbon.ui.internal.CarbonUIServiceComponent;
import org.wso2.carbon.utils.CarbonUtils;
import org.wso2.carbon.utils.FileItemData;
import org.wso2.carbon.utils.FileManipulator;
import org.wso2.carbon.utils.ServerConstants;
import org.wso2.carbon.utils.multitenancy.MultitenantConstants;

import javax.activation.DataHandler;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * This class is responsible for handling File uploads of specific file types in Carbon. The file
 * type is decided depending on the file extensio.
 * 
 * This class should be extended by all FileUploadExecutors
 *
 * @see FileUploadExecutorManager
 */
public abstract class AbstractFileUploadExecutor {

    protected static final Log log = LogFactory.getLog(AbstractFileUploadExecutor.class);

    protected ConfigurationContext configurationContext;

    private ThreadLocal<Map<String, ArrayList<FileItemData>>> fileItemsMap =
            new ThreadLocal<Map<String, ArrayList<FileItemData>>>();

    private ThreadLocal<Map<String, ArrayList<String>>> formFieldsMap =
            new ThreadLocal<Map<String, ArrayList<String>>>();
    
    private static final int DEFAULT_TOTAL_FILE_SIZE_LIMIT_IN_MB = 100;

    public abstract boolean execute(HttpServletRequest request,
                                    HttpServletResponse response) throws CarbonException,
                                                                         IOException;

    /**
     * Total allowed file upload size in bytes
     */
    private long totalFileUploadSizeLimit;

    protected AbstractFileUploadExecutor() {
        totalFileUploadSizeLimit = getFileSizeLimit();
    }

    private long getFileSizeLimit() {
        String totalFileSizeLimitInBytes =
                CarbonUIServiceComponent.getServerConfiguration().
                        getFirstProperty("FileUploadConfig.TotalFileSizeLimit");
        return totalFileSizeLimitInBytes != null ?
               Long.parseLong(totalFileSizeLimitInBytes) * 1024 * 1024 :
               DEFAULT_TOTAL_FILE_SIZE_LIMIT_IN_MB * 1024 * 1024;
    }

    boolean executeGeneric(HttpServletRequest request,
                           HttpServletResponse response,
                           ConfigurationContext configurationContext) throws IOException {//,
        //    CarbonException {
        this.configurationContext = configurationContext;
        try {
            parseRequest(request);
            return execute(request, response);
        } catch (FileUploadFailedException e) {
            sendErrorRedirect(request, response, e);
        } catch (FileSizeLimitExceededException e) {
            sendErrorRedirect(request, response, e);
        } catch (CarbonException e) {
            sendErrorRedirect(request, response, e);
        }
        return false;
    }

    private void sendErrorRedirect(HttpServletRequest request,
                                   HttpServletResponse response,
                                   Exception e) throws IOException {
        String errorRedirectionPage = getErrorRedirectionPage();
        if (errorRedirectionPage != null) {
            CarbonUIMessage.sendCarbonUIMessage(e.getMessage(), CarbonUIMessage.ERROR, request,
                                                response, errorRedirectionPage);        //TODO: error msg i18n
        } else {
            throw new IOException("Could not send error. " +
                                  "Please define the errorRedirectionPage in your UI page. " +
                                  e.getMessage());
        }
    }


    protected String getErrorRedirectionPage() {
        List<String> errRedirValues = getFormFieldValue("errorRedirectionPage");
        String errorRedirectionPage = null;
        if (errRedirValues != null && !errRedirValues.isEmpty()) {
            errorRedirectionPage = errRedirValues.get(0);
        }
        return errorRedirectionPage;
    }

    protected void parseRequest(HttpServletRequest request) throws FileUploadFailedException,
                                                                 FileSizeLimitExceededException {
        fileItemsMap.set(new HashMap<String, ArrayList<FileItemData>>());
        formFieldsMap.set(new HashMap<String, ArrayList<String>>());

        ServletRequestContext servletRequestContext = new ServletRequestContext(request);
        boolean isMultipart = ServletFileUpload.isMultipartContent(servletRequestContext);
        Long totalFileSize = 0L;

        if (isMultipart) {

            List items;
            try {
                items = parseRequest(servletRequestContext);
            } catch (FileUploadException e) {
                String msg = "File upload failed";
                log.error(msg, e);
                throw new FileUploadFailedException(msg, e);
            }
            boolean multiItems = false;
            if (items.size() > 1) {
                multiItems = true;
            }

            // Add the uploaded items to the corresponding maps.
            for (Iterator iter = items.iterator(); iter.hasNext();) {
                FileItem item = (FileItem) iter.next();
                String fieldName = item.getFieldName().trim();
                if (item.isFormField()) {
                    if (formFieldsMap.get().get(fieldName) == null) {
                        formFieldsMap.get().put(fieldName, new ArrayList<String>());
                    }
                    try {
                        formFieldsMap.get().get(fieldName).add(new String(item.get(), "UTF-8"));
                    } catch (UnsupportedEncodingException ignore) {
                    }
                } else {
                    String fileName = item.getName();
                    if ((fileName == null || fileName.length() == 0) && multiItems) {
                        continue;
                    }
                    if (fileItemsMap.get().get(fieldName) == null) {
                        fileItemsMap.get().put(fieldName, new ArrayList<FileItemData>());
                    }
                    totalFileSize += item.getSize();
                    if (totalFileSize < totalFileUploadSizeLimit) {
                        fileItemsMap.get().get(fieldName).add(new FileItemData(item));
                    } else {
                        throw new FileSizeLimitExceededException(getFileSizeLimit() / 1024 / 1024);
                    }
                }
            }
        }
    }

    protected String getWorkingDir() {
        return (String) configurationContext.getProperty(ServerConstants.WORK_DIR);
    }

    protected String generateUUID() {
        return String.valueOf(System.currentTimeMillis() + Math.random());
    }

    protected String getFileName(String fileName) {
        String fileNameOnly;
        if (fileName.indexOf("\\") < 0) {
            fileNameOnly = fileName.substring(fileName.lastIndexOf('/') + 1,
                                              fileName.length());
        } else {
            fileNameOnly = fileName.substring(fileName.lastIndexOf("\\") + 1,
                                              fileName.length());
        }
        return fileNameOnly;
    }

    protected List parseRequest(ServletRequestContext requestContext) throws FileUploadException {
        // Create a factory for disk-based file items
        FileItemFactory factory = new DiskFileItemFactory();
        // Create a new file upload handler
        ServletFileUpload upload = new ServletFileUpload(factory);
        // Parse the request
        return upload.parseRequest(requestContext);
    }

    //Old methods, these should be refactored.

    protected void checkServiceFileExtensionValidity(String fileExtension,
                                                     String[] allowedExtensions)
            throws FileUploadException {
        boolean isExtensionValid = false;
        StringBuffer allowedExtensionsStr = new StringBuffer();
        for (String allowedExtension : allowedExtensions) {
            allowedExtensionsStr.append(allowedExtension).append(",");
            if (fileExtension.endsWith(allowedExtension)) {
                isExtensionValid = true;
                break;
            }
        }
        if (!isExtensionValid) {
            throw new FileUploadException(" Illegal file type." +
                                          " Allowed file extensions are " + allowedExtensionsStr);
        }
    }

    protected File uploadFile(HttpServletRequest request,
                              String repoDir,
                              HttpServletResponse response,
                              String extension) throws FileUploadException {

        response.setContentType("text/html; charset=utf-8");
        ServletRequestContext servletRequestContext = new ServletRequestContext(request);
        boolean isMultipart =
                ServletFileUpload.isMultipartContent(servletRequestContext);
        File uploadedFile = null;
        if (isMultipart) {
            try {
                // Create a new file upload handler
                List items = parseRequest(servletRequestContext);

                // Process the uploaded items
                for (Iterator iter = items.iterator(); iter.hasNext();) {
                    FileItem item = (FileItem) iter.next();
                    if (!item.isFormField()) {
                        String fileName = item.getName();
                        String fileExtension = fileName;
                        fileExtension = fileExtension.toLowerCase();
                        if (extension != null && !fileExtension.endsWith(extension)) {
                            throw new Exception(" Illegal file type. Only " +
                                                extension + " files can be uploaded");

                        }
                        String fileNameOnly = getFileName(fileName);
                        uploadedFile = new File(repoDir, fileNameOnly);
                        item.write(uploadedFile);
                    }
                }
            } catch (Exception e) {
                String msg = "File upload failed";
                log.error(msg, e);
                throw new FileUploadException(msg, e);
            }
        }
        return uploadedFile;
    }

    /**
     * This is the common method that can be used for Fileupload.
     * extraStoreDirUUID is the name of the javascript that's going to
     * execute on the client side at the secound run.
     *
     * @param request
     * @param response
     * @return Status true/fase.
     * @throws org.apache.commons.fileupload.FileUploadException
     *
     */
    protected boolean executeCommon(HttpServletRequest request, HttpServletResponse response)
            throws FileUploadException {

        String serverURL = (String) request.getAttribute(CarbonConstants.SERVER_URL);
        HttpSession session = request.getSession();
        String cookie = (String) session.getAttribute(ServerConstants.ADMIN_SERVICE_COOKIE);

        PrintWriter out = null;
        try {
            out = response.getWriter();
            FileUploadServiceClient client =
                    new FileUploadServiceClient(configurationContext, serverURL, cookie);

            response.setContentType("text/plain; charset=utf-8");
            Set<String> keys = fileItemsMap.get().keySet();
            boolean multiItems = false;
            if (fileItemsMap.get().size() > 1) {
                multiItems = true;
            }
            // Process the uploaded items
            UploadedFileItem[] uploadedFileItems = new UploadedFileItem[fileItemsMap.get().size()];
            Iterator<String> iterator = keys.iterator();
            int i = 0;
            while (iterator.hasNext()) {
                String fieldName = iterator.next();
                String fileName = fileItemsMap.get().get(fieldName).get(0).getFileItem().getName();
                if ((fileName == null || fileName.length() == 0) && multiItems) {
                    continue;
                }
                FileItemData fileItemData = fileItemsMap.get().get(fieldName).get(0);
                UploadedFileItem uploadedFileItem = new UploadedFileItem();
                uploadedFileItem.setDataHandler(fileItemData.getDataHandler());
                uploadedFileItem.setFileName(fileName);
                uploadedFileItem.setFileType("");
                uploadedFileItems[i] = uploadedFileItem;
                i++;
            }
            String[] uuidArray = client.uploadFiles(uploadedFileItems);
            StringBuffer uuids = new StringBuffer();
            for (String uuid : uuidArray) {
                uuids.append(uuid).append(",");
            }
            out.write(uuids.toString().substring(0, uuids.length() - 1));
            out.flush();
        } catch (Exception e) {
            String msg = "File upload FAILED. File may be non-existent or invalid.";
            log.error(msg, e);
            throw new FileUploadException(msg, e);
        } finally {
            if (out != null) {
                out.close();
            }
        }
        return true;
    }


    /**
     * This is a helper method that will be used upload main entity (ex: wsdd, jar, class etc) and
     * its resources to a given deployer.
     *
     * @param request
     * @param response
     * @param uploadDirName
     * @param extensions
     * @param utilityString
     * @return boolean
     * @throws IOException
     */
    protected boolean uploadArtifacts(HttpServletRequest request,
                                      HttpServletResponse response,
                                      String uploadDirName,
                                      String[] extensions,
                                      String utilityString)
            throws FileUploadException, IOException {
        String axis2Repo = ServerConfiguration.getInstance().
                getFirstProperty(ServerConfiguration.AXIS2_CONFIG_REPO_LOCATION);
        if (CarbonUtils.isURL(axis2Repo)) {
            String msg = "You are not permitted to upload jars to URL repository";
            throw new FileUploadException(msg);
        }

        String tmpDir = (String) configurationContext.getProperty(ServerConstants.WORK_DIR);
        String uuid = String.valueOf(System.currentTimeMillis() + Math.random());
        tmpDir = tmpDir + File.separator + "artifacts" + File.separator + uuid + File.separator;
        File tmpDirFile = new File(tmpDir);
        if (!tmpDirFile.exists() && !tmpDirFile.mkdirs()) {
            log.warn("Could not create " + tmpDirFile.getAbsolutePath());
        }

        response.setContentType("text/html; charset=utf-8");

        ServletRequestContext servletRequestContext = new ServletRequestContext(request);
        boolean isMultipart =
                ServletFileUpload.isMultipartContent(servletRequestContext);
        if (isMultipart) {
            PrintWriter out = null;
            try {
                out = response.getWriter();
                // Create a new file upload handler
                List items = parseRequest(servletRequestContext);
                // Process the uploaded items
                for (Iterator iter = items.iterator(); iter.hasNext();) {
                    FileItem item = (FileItem) iter.next();
                    if (!item.isFormField()) {
                        String fileName = item.getName();
                        String fileExtension = fileName;
                        fileExtension = fileExtension.toLowerCase();

                        String fileNameOnly = getFileName(fileName);
                        File uploadedFile;

                        String fieldName = item.getFieldName();

                        if (fieldName != null && fieldName.equals("jarResource")) {
                            if (fileExtension.endsWith(".jar")) {
                                File servicesDir =
                                        new File(tmpDir + File.separator + uploadDirName, "lib");
                                if (!servicesDir.exists() && !servicesDir.mkdirs()){
                                    log.warn("Could not create " + servicesDir.getAbsolutePath());
                                }
                                uploadedFile = new File(servicesDir, fileNameOnly);
                                item.write(uploadedFile);
                            }
                        } else {
                            File servicesDir = new File(tmpDir, uploadDirName);
                            if (!servicesDir.exists() && !servicesDir.mkdirs()) {
                                log.warn("Could not create " + servicesDir.getAbsolutePath());
                            }
                            uploadedFile = new File(servicesDir, fileNameOnly);
                            item.write(uploadedFile);
                        }
                    }
                }

                //First lets filter for jar resources
                String repo = configurationContext.getAxisConfiguration().getRepository().getPath();

                //Writing the artifacts to the proper location
                String parent = repo + File.separator + uploadDirName;
                File mainDir = new File(tmpDir + File.separator + uploadDirName);
                File libDir = new File(mainDir, "lib");
                File[] resourceLibFile =
                        FileManipulator.getMatchingFiles(libDir.getAbsolutePath(), null, "jar");


                for (File src : resourceLibFile) {
                    File dst = new File(parent, "lib");
                    String[] files = libDir.list();
                    for (String file : files) {
                        copyFile(src, new File(dst, file));
                    }
                }

                for (String extension : extensions) {
                    File[] mainFiles =
                            FileManipulator.getMatchingFiles(mainDir.getAbsolutePath(), null, extension);
                    for (File mainFile : mainFiles) {
                        File dst = new File(parent);
                        String[] files = mainDir.list();
                        for (String file : files) {
                            File f = new File(dst, file);
                            if (!f.isDirectory()) {
                                copyFile(mainFile, f);
                            }
                        }

                    }
                }
                response.sendRedirect(getContextRoot(request) + "/carbon/service-mgt/index.jsp?message=Files have been uploaded "
                                      + "successfully. This page will be auto refreshed shortly with "
                                      + "the status of the created " + utilityString + " service"); //TODO: why do we redirect to service-mgt ???
                return true;
            } catch (RuntimeException e) {
                throw e;
            } catch (Exception e) {
                String msg = "File upload failed";
                log.error(msg, e);
                throw new FileUploadException(msg, e);
            } finally {
                if (out != null) {
                    out.close();
                }
            }
        }
        return false;
    }

    private void copyFile(File src, File dst) throws IOException {
        String dstAbsPath = dst.getAbsolutePath();
        String dstDir = dstAbsPath.substring(0, dstAbsPath.lastIndexOf(File.separator));
        File dir = new File(dstDir);
        if (!dir.exists() && !dir.mkdirs()) {
            log.warn("Could not create " + dir.getAbsolutePath());
        }
        DataHandler dh = new DataHandler(src.toURL());
        FileOutputStream out = null;
        try {
            out = new FileOutputStream(dst);
            dh.writeTo(out);
            out.flush();
        } finally {
            if (out != null) {
                out.close();
            }
        }
    }

    protected List<FileItemData> getAllFileItems() {
        Collection<ArrayList<FileItemData>> listCollection = fileItemsMap.get().values();
        List<FileItemData> fileItems = new ArrayList<FileItemData>();
        for (ArrayList<FileItemData> fileItemData : listCollection) {
            fileItems.addAll(fileItemData);
        }
        return fileItems;
    }

    protected String getContextRoot(HttpServletRequest request) {
        String contextPath = (request.getContextPath().equals("")) ? "" : request.getContextPath();
        int index;
        if (contextPath.equals("/fileupload")) {
            contextPath = "";
        } else {
            if ((index = contextPath.indexOf("/fileupload")) > -1) {
                contextPath = contextPath.substring(0, index);
            }
        }
        // Make the context root tenant aware, eg: /t/wso2.com in a multi-tenant scenario
        String tenantDomain = (String)request.getSession().getAttribute(MultitenantConstants.TENANT_DOMAIN);
        if(!contextPath.startsWith("/" + MultitenantConstants.TENANT_AWARE_URL_PREFIX + "/")
                && (tenantDomain != null &&
                !MultitenantConstants.SUPER_TENANT_DOMAIN_NAME.equals(tenantDomain)) ){
            contextPath = contextPath + "/" + MultitenantConstants.TENANT_AWARE_URL_PREFIX + "/" +
                          tenantDomain;
            // replace the possible '//' with '/ '
            contextPath = contextPath.replaceAll("//", "/");
        }
        return contextPath;
    }

    /**
     * Retrieve the form field values of the provided form field with name <code>formFieldName</code>
     *
     * @param formFieldName Name of the form field to be retrieved
     * @return List of form field values
     */
    public List<String> getFormFieldValue(String formFieldName) {
        return formFieldsMap.get().get(formFieldName);
    }

    protected Map<String, ArrayList<FileItemData>> getFileItemsMap() {
        return fileItemsMap.get();
    }

    protected Map<String, ArrayList<String>> getFormFieldsMap() {
        return formFieldsMap.get();
    }
}
