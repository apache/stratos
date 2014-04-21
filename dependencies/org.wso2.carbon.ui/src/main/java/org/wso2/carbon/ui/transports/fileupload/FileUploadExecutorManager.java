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

import org.apache.axiom.om.OMElement;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.util.XMLUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.wso2.carbon.CarbonConstants;
import org.wso2.carbon.CarbonException;
import org.wso2.carbon.base.ServerConfiguration;
import org.wso2.carbon.ui.CarbonUIUtil;
import org.wso2.carbon.utils.ServerConstants;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.xml.namespace.QName;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * <p>
 * This class is responsible for delegating the file upload requests to the different
 * FileUploadExecutors.
 * </p>
 * <p>
 * The FileUploadExecutors may be registered in the following manner:
 *
 * <ol>
 *      <li>1. Using FileUploadConfig configuration section in the carbon.xml</li>
 *      <li>2. Instances of {@link AbstractFileUploadExecutor } registered as OSGi services</li>
 *      <li>3. Using component.xml file in UI components</li>
 * </ol>
 * </p>
 * <p>
 * If a FileUploadExecutor cannot be found in the above 3 collections, as a final resort,
 * we will finally try to upload the file using an {@link AnyFileUploadExecutor}, if it has been
 * registered in the carbon.xml file. Searching for an FileUploadExecutor in the above 3 types of
 * items is done using the chain of execution pattern. As soon as a FileUploadExecutor which can
 * handle the uploaded file type is found, execution of the chain is terminated.
 * </p>
 */
public class FileUploadExecutorManager {

    private static Log log = LogFactory.getLog(FileUploadExecutorManager.class);

    private Map<String, AbstractFileUploadExecutor> executorMap =
            new HashMap<String, AbstractFileUploadExecutor>();

    private BundleContext bundleContext;

    private ConfigurationContext configContext;

    private String webContext;

    public FileUploadExecutorManager(BundleContext bundleContext,
                                     ConfigurationContext configCtx,
                                     String webContext) throws CarbonException {
        this.bundleContext = bundleContext;
        this.configContext = configCtx;
        this.webContext = webContext;
        this.loadExecutorMap();
    }

    /**
     * When a FileUpload request is received, this method will be called.
     *
     * @param request The HTTP Request
     * @param response  The HTTP Response
     * @return true - if the file uploading was successful, false - otherwise
     * @throws IOException If an unrecoverable error occurs during file upload
     */
    public boolean execute(HttpServletRequest request,
                           HttpServletResponse response) throws IOException {

        HttpSession session = request.getSession();
        String cookie = (String) session.getAttribute(ServerConstants.ADMIN_SERVICE_COOKIE);
        request.setAttribute(CarbonConstants.ADMIN_SERVICE_COOKIE, cookie);
        request.setAttribute(CarbonConstants.WEB_CONTEXT, webContext);
        request.setAttribute(CarbonConstants.SERVER_URL,
                             CarbonUIUtil.getServerURL(request.getSession().getServletContext(),
                                                       request.getSession()));


        String requestURI = request.getRequestURI();

        //TODO - fileupload is hardcoded
        int indexToSplit = requestURI.indexOf("fileupload/") + "fileupload/".length();
        String actionString = requestURI.substring(indexToSplit);

        // Register execution handlers
        FileUploadExecutionHandlerManager execHandlerManager =
                new FileUploadExecutionHandlerManager();
        CarbonXmlFileUploadExecHandler carbonXmlExecHandler =
                new CarbonXmlFileUploadExecHandler(request, response, actionString);
        execHandlerManager.addExecHandler(carbonXmlExecHandler);
        OSGiFileUploadExecHandler osgiExecHandler =
                new OSGiFileUploadExecHandler(request, response);
        execHandlerManager.addExecHandler(osgiExecHandler);
        AnyFileUploadExecHandler anyFileExecHandler =
                new AnyFileUploadExecHandler(request, response);
        execHandlerManager.addExecHandler(anyFileExecHandler);
        execHandlerManager.startExec();
        return true;
    }

    private void loadExecutorMap() throws CarbonException {
        ServerConfiguration serverConfiguration = ServerConfiguration.getInstance();
        OMElement documentElement;
        try {
            documentElement = XMLUtils.toOM(serverConfiguration.getDocumentElement());
        } catch (Exception e) {
            String msg = "Unable to read Server Configuration.";
            log.error(msg);
            throw new CarbonException(msg, e);
        }
        OMElement fileUploadConfigElement =
                documentElement.getFirstChildWithName(
                        new QName(ServerConstants.CARBON_SERVER_XML_NAMESPACE, "FileUploadConfig"));
        for (Iterator iterator = fileUploadConfigElement.getChildElements(); iterator.hasNext();) {
            OMElement mapppingElement = (OMElement) iterator.next();
            if (mapppingElement.getLocalName().equalsIgnoreCase("Mapping")) {
                OMElement actionsElement =
                        mapppingElement.getFirstChildWithName(
                                new QName(ServerConstants.CARBON_SERVER_XML_NAMESPACE, "Actions"));

                if (actionsElement == null) {
                    String msg = "The mandatory FileUploadConfig/Actions entry " +
                                 "does not exist or is empty in the CARBON_HOME/conf/carbon.xml " +
                                 "file. Please fix this error in the  carbon.xml file and restart.";
                    log.error(msg);
                    throw new CarbonException(msg);
                }
                Iterator actionElementIterator =
                        actionsElement.getChildrenWithName(
                                new QName(ServerConstants.CARBON_SERVER_XML_NAMESPACE, "Action"));

                if (!actionElementIterator.hasNext()) {
                    String msg = "A FileUploadConfig/Mapping entry in the " +
                                 "CARBON_HOME/conf/carbon.xml should have at least on Action " +
                                 "defined. Please fix this error in the carbon.xml file and " +
                                 "restart.";
                    log.error(msg);
                    throw new CarbonException(msg);
                }

                OMElement classElement = mapppingElement.getFirstChildWithName(
                        new QName(ServerConstants.CARBON_SERVER_XML_NAMESPACE, "Class"));

                if (classElement == null || classElement.getText() == null) {
                    String msg = "The mandatory FileUploadConfig/Mapping/Class entry " +
                                 "does not exist or is empty in the CARBON_HOME/conf/carbon.xml " +
                                 "file. Please fix this error in the  carbon.xml file and restart.";
                    log.error(msg);
                    throw new CarbonException(msg);
                }


                AbstractFileUploadExecutor object;
                String className = classElement.getText().trim();

                try {
                    Class clazz = bundleContext.getBundle().loadClass(className);
                    Constructor constructor =
                            clazz.getConstructor();
                    object = (AbstractFileUploadExecutor) constructor
                            .newInstance();

                } catch (Exception e) {
                    String msg = "Error occurred while trying to instantiate the " + className +
                                 " class specified as a FileUploadConfig/Mapping/class element in " +
                                 "the CARBON_HOME/conf/carbon.xml file. Please fix this error in " +
                                 "the carbon.xml file and restart.";
                    log.error(msg, e);
                    throw new CarbonException(msg, e);
                }

                while (actionElementIterator.hasNext()) {
                    OMElement actionElement = (OMElement) actionElementIterator.next();
                    if (actionElement.getText() == null) {
                        String msg = "A FileUploadConfig/Mapping/Actions/Action element in the " +
                                     "CARBON_HOME/conf/carbon.xml file is empty. Please include " +
                                     "the correct value in this file and restart.";
                        log.error(msg);
                        throw new CarbonException(msg);
                    }
                    executorMap.put(actionElement.getText().trim(), object);
                }
            }
        }
    }

    public void addExecutor(String action, String executorClass) throws CarbonException {
        if (action == null) {
            String msg = "A FileUploadConfig/Mapping/Actions/Action element is null ";
            log.error(msg);
        }

        if (executorClass == null || executorClass.equals("")) {
            String msg = "Provided FileUploadExecutor object is invalid ";
            log.error(msg);
        }

        AbstractFileUploadExecutor object;

        try {
            Class clazz = bundleContext.getBundle().loadClass(executorClass);
            Constructor constructor = clazz.getConstructor();
            object = (AbstractFileUploadExecutor) constructor.newInstance();
            executorMap.put(action, object);
        } catch (Exception e) {
            String msg = "Error occurred while trying to instantiate the " + executorClass +
                         " class specified as a FileUploadConfig/Mapping/class element";
            log.error(msg, e);
            throw new CarbonException(msg, e);
        }
    }

    public void removeExecutor(String action) {
        if (action == null) {
            String msg = "A FileUploadConfig/Mapping/Actions/Action element is null ";
            log.error(msg);
        }

        executorMap.remove(action);
    }

    /**
     * This class manages registration of a chain of {@link FileUploadExecutionHandler}s. Uses
     * chain of execution pattern.
     */
    private static class FileUploadExecutionHandlerManager {

        /**
         * First handler in the chain
         */
        private FileUploadExecutionHandler firstHandler;

        /**
         * Previous handler
         */
        private FileUploadExecutionHandler prevHandler;

        public void addExecHandler(FileUploadExecutionHandler handler) {
            if (prevHandler != null) {
                prevHandler.setNext(handler);
            } else {
                firstHandler = handler;
            }
            prevHandler = handler;
        }

        public void startExec() throws IOException {
            firstHandler.execute();
        }
    }

    /**
     * The base class of all FileUploadExecutionHandler. For each type of file upload execution
     * chain, we will implement a subclass of this. Uses chain of execution pattern.
     */
    private abstract class FileUploadExecutionHandler {
        private FileUploadExecutionHandler next;

        public abstract void execute() throws IOException;

        public final void next() throws IOException {
            next.execute();
        }

        public final void setNext(FileUploadExecutionHandler next) {
            this.next = next;
        }
    }

    /**
     * Represents upload execution chain of {@link AnyFileUploadExecutor}
     */
    private class AnyFileUploadExecHandler extends FileUploadExecutionHandler {
        private HttpServletRequest request;
        private HttpServletResponse response;

        private AnyFileUploadExecHandler(HttpServletRequest request, HttpServletResponse response) {
            this.request = request;
            this.response = response;
        }

        @Override
        public void execute() throws IOException {
            Object obj = executorMap.get("*");
            if (obj == null) {
                log.warn("Reached 'All' section but Could not find the Implementation Class");
                return;
            }
            AnyFileUploadExecutor executor = (AnyFileUploadExecutor) obj;
            executor.executeGeneric(request, response, configContext);
        }
    }

    /**
     * Represents upload execution chain of UploadExecutors registered as OSGi services
     */
    private class OSGiFileUploadExecHandler extends FileUploadExecutionHandler {

        private HttpServletRequest request;
        private HttpServletResponse response;

        private OSGiFileUploadExecHandler(HttpServletRequest request,
                                          HttpServletResponse response) {
            this.request = request;
            this.response = response;
        }

        public void execute() throws IOException {

            ServiceReference[] serviceReferences;
            try {
                serviceReferences =
                        bundleContext.
                                getServiceReferences(AbstractFileUploadExecutor.class.getName(),
                                                     null);
            } catch (InvalidSyntaxException e) {
                throw new IllegalArgumentException("Service reference cannot be obtained", e);
            }
            boolean foundExecutor = false;
            if (serviceReferences != null) {
                String requestURI = request.getRequestURI();
                for (ServiceReference reference : serviceReferences) {
                    String action = (String) reference.getProperty("action");
                    if (action != null && requestURI.indexOf(action) > -1) {
                        foundExecutor = true;
                        AbstractFileUploadExecutor uploadExecutor =
                                (AbstractFileUploadExecutor) bundleContext.getService(reference);
                        uploadExecutor.executeGeneric(request, response, configContext);
                        break;
                    }
                }
                if (!foundExecutor) {
                    next();
                }
            }

        }
    }

    /**
     * Represents upload execution chain of UploadExecutors registered in carbon.xml, except for
     * {@link AnyFileUploadExecutor}
     */
    private class CarbonXmlFileUploadExecHandler extends FileUploadExecutionHandler {

        private HttpServletRequest request;
        private HttpServletResponse response;
        private String actionString;

        private CarbonXmlFileUploadExecHandler(HttpServletRequest request,
                                               HttpServletResponse response,
                                               String actionString) {
            this.request = request;
            this.response = response;
            this.actionString = actionString;
        }

        public void execute() throws IOException {
            boolean foundExecutor = false;
            for (String key : executorMap.keySet()) {
                if (key.equals(actionString)) {
                    AbstractFileUploadExecutor obj = executorMap.get(key);
                    foundExecutor = true;
                    obj.executeGeneric(request, response, configContext);
                    break;
                }
            }
            if (!foundExecutor) {
                next();
            }
        }
    }
}
