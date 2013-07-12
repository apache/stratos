/**
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at

 *  http://www.apache.org/licenses/LICENSE-2.0

 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

package org.apache.stratos.theme.mgt.ui.processors;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.CarbonConstants;
import org.wso2.carbon.CarbonException;
import org.wso2.carbon.registry.core.RegistryConstants;
import org.apache.stratos.theme.mgt.ui.clients.ThemeMgtServiceClient;
import org.wso2.carbon.ui.CarbonUIMessage;
import org.wso2.carbon.ui.transports.fileupload.AbstractFileUploadExecutor;
import org.wso2.carbon.utils.FileItemData;
import org.wso2.carbon.utils.ServerConstants;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.imageio.ImageIO;
import javax.mail.util.ByteArrayDataSource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;


public class AddThemeResourceProcessor extends AbstractFileUploadExecutor {

    private static final Log log = LogFactory.getLog(AddThemeResourceProcessor.class);

    public boolean execute(HttpServletRequest request, HttpServletResponse response)
            throws CarbonException, IOException {

        String webContext = (String) request.getAttribute(CarbonConstants.WEB_CONTEXT);
        String serverURL = (String) request.getAttribute(CarbonConstants.SERVER_URL);
        String cookie = (String) request.getAttribute(ServerConstants.ADMIN_SERVICE_COOKIE);
        // HttpSession session = request.getSession();

        Map<String, ArrayList<FileItemData>> fileItemsMap = getFileItemsMap();
        if (fileItemsMap == null || fileItemsMap.isEmpty()) {
            String msg = "File uploading failed. Content is not set properly.";
            log.error(msg);

            CarbonUIMessage.sendCarbonUIMessage(msg, CarbonUIMessage.ERROR, request);
            response.sendRedirect(
                    "../" + webContext + "/admin/error.jsp");

            return false;
        }

        try {
            ThemeMgtServiceClient client =
                    new ThemeMgtServiceClient(cookie, serverURL, configurationContext);

            String parentPath = null;
            Map<String, ArrayList<java.lang.String>> formFieldsMap = getFormFieldsMap();
            if (formFieldsMap.get("path") != null) {
                parentPath = formFieldsMap.get("path").get(0);
            }
            String resourceName = null;
            if (formFieldsMap.get("filename") != null) {
                resourceName = formFieldsMap.get("filename").get(0);
            }
            String mediaType = null;
            if (formFieldsMap.get("mediaType") != null) {
                mediaType = formFieldsMap.get("mediaType").get(0);
            }
            String description = null;
            if (formFieldsMap.get("description") != null) {
                description = formFieldsMap.get("description").get(0);
            }
            String symlinkLocation = null;
            if (formFieldsMap.get("symlinkLocation") != null) {
                symlinkLocation = formFieldsMap.get("symlinkLocation").get(0);
            }
            String redirectWith = null;
            if (formFieldsMap.get("redirectWith") != null) {
                redirectWith = formFieldsMap.get("redirectWith").get(0);
            }
            /*
            // currently chroot will not work with multitenancy
            IServerAdmin adminClient =
                    (IServerAdmin) CarbonUIUtil.
                            getServerProxy(new ServerAdminClient(configurationContext,
                                    serverURL, cookie, session), IServerAdmin.class, session);
            ServerData data = adminClient.getServerData();
            String chroot = "";
            if (data.getRegistryType().equals("remote") && data.getRemoteRegistryChroot() != null &&
                    !data.getRemoteRegistryChroot().equals(RegistryConstants.PATH_SEPARATOR)) {
                chroot = data.getRemoteRegistryChroot();
                if (!chroot.startsWith(RegistryConstants.PATH_SEPARATOR)) {
                    chroot = RegistryConstants.PATH_SEPARATOR + chroot;
                }
                if (chroot.endsWith(RegistryConstants.PATH_SEPARATOR)) {
                    chroot = chroot.substring(0, chroot.length() - RegistryConstants.PATH_SEPARATOR.length());
                }
            }
            if (symlinkLocation != null) {
                symlinkLocation = chroot + symlinkLocation;
            }
            */

            FileItemData fileItemData = fileItemsMap.get("upload").get(0);

            if ((fileItemData == null) || (fileItemData.getFileItem().getSize() == 0)) {
                String msg = "Failed add resource. Resource content is empty.";
                log.error(msg);

                CarbonUIMessage.sendCarbonUIMessage(msg, CarbonUIMessage.ERROR, request);
                response.sendRedirect(
                        "../" + webContext + "/admin/error.jsp");

                return false;
            }
            DataHandler dataHandler = scaleImage(fileItemData.getDataHandler(), 48, 119);

            client.addResource(
                    calculatePath(parentPath, resourceName), mediaType, description, dataHandler,
                    symlinkLocation, redirectWith);

            response.setContentType("text/html; charset=utf-8");
            String msg = "The logo has been successfully updated.";
            CarbonUIMessage.sendCarbonUIMessage(msg, CarbonUIMessage.INFO, request);

            String redirectTo = request.getParameter("redirectto");
            if ("theme_mgt".equals(redirectTo)) {
                response.setHeader("Cache-Control", "no-cache, must-revalidate");
                response.sendRedirect("../" + webContext +
                        "/tenant-theme/theme_mgt.jsp?logoChanged=true&redirectWith=" + redirectWith);
            }else if ("logo_mgt".equals(redirectTo)) {
                response.setHeader("Cache-Control", "no-cache, must-revalidate");
                response.sendRedirect("../" + webContext +
                        "/tenant-theme/logo_mgt.jsp?logoChanged=true&redirectWith=" + redirectWith);
            }
            else {
                response.sendRedirect("../" + webContext + "/tenant-theme/theme_advanced.jsp?path=" + parentPath);
            }
            return true;

        } catch (Exception e) {
            String msg = "File upload failed. Please confirm that the chosen image is not corrupted " +
                    "and retry uploading, or upload a valid image.";
            log.error(msg + " " + e.getMessage());

            CarbonUIMessage.sendCarbonUIMessage(msg, CarbonUIMessage.ERROR, request);
            response.sendRedirect(
                    "../" + webContext + "/admin/error.jsp");

            return false;
        }
    }

    private static DataHandler scaleImage(DataHandler dataHandler, int height, int width) throws IOException {

        Image image = ImageIO.read(new BufferedInputStream(dataHandler.getInputStream()));
        // Check if the image has transparent pixels
        boolean hasAlpha = ((BufferedImage)image).getColorModel().hasAlpha();

        // Maintain Aspect ratio
        int thumbHeight = height;
        int thumbWidth = width;
        double thumbRatio = (double)width / (double)height;
        double imageRatio = (double)image.getWidth(null) / (double)image.getHeight(null);
        if (thumbRatio < imageRatio) {
            thumbHeight = (int)(thumbWidth / imageRatio);
        } else {
            thumbWidth = (int)(thumbHeight * imageRatio);
        }

        BufferedImage thumb;
        // Check if transparent pixels are available and set the color mode accordingly 
        if (hasAlpha) {
            thumb = new BufferedImage(thumbWidth, thumbHeight, BufferedImage.TYPE_INT_ARGB);
        } else {
            thumb = new BufferedImage(thumbWidth, thumbHeight, BufferedImage.TYPE_INT_RGB);
        }
        Graphics2D graphics2D = thumb.createGraphics();
        graphics2D.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                                    RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        graphics2D.drawImage(image, 0, 0, thumbWidth, thumbHeight, null);

        // Save the image as PNG so that transparent images are rendered as intended
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ImageIO.write(thumb, "PNG", output);

        DataSource dataSource= new ByteArrayDataSource(output.toByteArray(), "application/octet-stream");
        return new DataHandler(dataSource);
    }

    private static String calculatePath(String parentPath, String resourceName) {
        String resourcePath;
        if (!parentPath.startsWith(RegistryConstants.PATH_SEPARATOR)) {
            parentPath = RegistryConstants.PATH_SEPARATOR + parentPath;
        }
        if (parentPath.endsWith(RegistryConstants.PATH_SEPARATOR)) {
            resourcePath = parentPath + resourceName;
        } else {
            resourcePath = parentPath + RegistryConstants.PATH_SEPARATOR + resourceName;
        }
        return resourcePath;
    }
}
