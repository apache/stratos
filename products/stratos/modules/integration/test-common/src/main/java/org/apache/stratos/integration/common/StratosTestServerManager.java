/*
 * Copyright 2005-2015 WSO2, Inc. (http://wso2.com)
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

package org.apache.stratos.integration.common;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.common.constants.StratosConstants;
import org.wso2.carbon.automation.engine.context.AutomationContext;
import org.wso2.carbon.automation.engine.context.beans.User;
import org.wso2.carbon.automation.engine.exceptions.AutomationFrameworkException;
import org.wso2.carbon.automation.engine.frameworkutils.CodeCoverageUtils;
import org.wso2.carbon.automation.engine.frameworkutils.FrameworkPathUtil;
import org.wso2.carbon.automation.engine.frameworkutils.ReportGenerator;
import org.wso2.carbon.automation.engine.frameworkutils.TestFrameworkUtils;
import org.wso2.carbon.automation.extensions.servers.carbonserver.CarbonServerManager;
import org.wso2.carbon.automation.extensions.servers.carbonserver.TestServerManager;
import org.wso2.carbon.automation.extensions.servers.utils.ArchiveExtractor;
import org.wso2.carbon.automation.extensions.servers.utils.ClientConnectionUtil;
import org.wso2.carbon.automation.extensions.servers.utils.FileManipulator;
import org.wso2.carbon.automation.extensions.servers.utils.ServerLogReader;

import javax.xml.xpath.XPathExpressionException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

/*
 * Manager class implementation for Carbon automation engine. It will copy the artifacts needed by Stratos and set the
 * relevant ports in configuration files in the runtime.
 */
public class StratosTestServerManager extends TestServerManager {
    private static final Log log = LogFactory.getLog(StratosTestServerManager.class);
    public static final String PATH_SEP = File.separator;
    public static final String MOCK_IAAS_XML_FILENAME = "mock-iaas.xml";
    public static final String SCALING_DROOL_FILENAME = "scaling.drl";
    public static final String JNDI_PROPERTIES_FILENAME = "jndi.properties";
    public static final String JMS_OUTPUT_ADAPTER_FILENAME = "JMSOutputAdaptor.xml";
    public static final String CLOUD_CONTROLLER_FILENAME = "cloud-controller.xml";
    public static final String AUTOSCALER_FILENAME = "autoscaler.xml";
    public static final String CARTRIDGE_CONFIG_PROPERTIES_FILENAME = "cartridge-config.properties";
    public static final String IDENTITY_FILENAME = "identity.xml";
    private static final String LOG4J_PROPERTIES_FILENAME = "log4j.properties";
    private static final String THRIFT_CLIENT_CONFIG_FILENAME = "thrift-client-config.xml";
    private int activeMQDynamicPort;
    private int stratosSecureDynamicPort;
    private int stratosDynamicPort;
    private int thriftDynamicPort;
    private int thriftSecureDynamicPort;
    private String webAppURL;
    private String webAppURLHttps;

    public StratosTestServerManager(AutomationContext context) {
        super(context);
        this.carbonServer = new StratosServerManager(context);
    }

    public StratosTestServerManager(AutomationContext context, String carbonZip, Map<String, String> commandMap) {
        super(context, carbonZip, commandMap);
        this.carbonServer = new StratosServerManager(context);
        this.carbonZip = carbonZip;
        if (commandMap.get("-DportOffset") != null) {
            this.portOffset = Integer.parseInt((String) commandMap.get("-DportOffset"));
            this.commandMap = commandMap;
        } else {
            throw new IllegalArgumentException("portOffset value must be set in command list");
        }
    }

    public StratosTestServerManager(AutomationContext context, int portOffset) {
        super(context, portOffset);
        this.carbonServer = new StratosServerManager(context);
        this.portOffset = portOffset;
        this.commandMap.put("-DportOffset", String.valueOf(portOffset));
    }

    public StratosTestServerManager(AutomationContext context, String carbonZip) {
        super(context, carbonZip);
        this.carbonServer = new StratosServerManager(context);
        this.carbonZip = carbonZip;
    }

    public String startServer() throws AutomationFrameworkException, IOException, XPathExpressionException {
        if (this.carbonHome == null) {
            if (this.carbonZip == null) {
                this.carbonZip = System.getProperty("carbon.zip");
            }

            if (this.carbonZip == null) {
                throw new IllegalArgumentException("carbon zip file cannot find in the given location");
            }

            this.carbonHome = this.carbonServer.setUpCarbonHome(this.carbonZip);
            this.configureServer();
        }

        log.info("Carbon Home - " + this.carbonHome);
        if (this.commandMap.get("-DportOffset") != null) {
            this.portOffset = Integer.parseInt((String) this.commandMap.get("-DportOffset"));
        } else {
            this.portOffset = 0;
        }

        this.carbonServer.startServerUsingCarbonHome(this.carbonHome, this.commandMap);
        return this.carbonHome;
    }

    public void configureServer() throws AutomationFrameworkException {
        try {
            log.info("Configuring server using CARBON_HOME: " + carbonHome);
            copyArtifacts();

            // set truststores and jndi.properties path
            setSystemproperties();
        } catch (IOException e) {
            throw new AutomationFrameworkException("Could not configure Stratos server", e);
        }
    }

    public void stopServer() throws AutomationFrameworkException {
        super.stopServer();
    }

    protected void copyArtifacts() throws IOException {
        String commonResourcesPath = Util.getCommonResourcesFolderPath();
        copyConfigFile(commonResourcesPath, MOCK_IAAS_XML_FILENAME, Util.CARBON_CONF_PATH);
        copyConfigFile(commonResourcesPath, JNDI_PROPERTIES_FILENAME, Util.CARBON_CONF_PATH);
        copyConfigFile(commonResourcesPath, LOG4J_PROPERTIES_FILENAME, Util.CARBON_CONF_PATH);
        copyConfigFile(commonResourcesPath, CLOUD_CONTROLLER_FILENAME, Util.CARBON_CONF_PATH);
        copyConfigFile(commonResourcesPath, AUTOSCALER_FILENAME, Util.CARBON_CONF_PATH);
        copyConfigFile(commonResourcesPath, CARTRIDGE_CONFIG_PROPERTIES_FILENAME, Util.CARBON_CONF_PATH);
        copyConfigFile(commonResourcesPath, IDENTITY_FILENAME, Util.CARBON_CONF_PATH);
        copyConfigFile(commonResourcesPath, THRIFT_CLIENT_CONFIG_FILENAME, Util.CARBON_CONF_PATH);
        copyConfigFile(commonResourcesPath, SCALING_DROOL_FILENAME, Util.CARBON_CONF_PATH + PATH_SEP + "drools");
        copyConfigFile(commonResourcesPath, JMS_OUTPUT_ADAPTER_FILENAME,
                "repository" + PATH_SEP + "deployment" + PATH_SEP + "server" + PATH_SEP + "outputeventadaptors");

    }

    private void copyConfigFile(String filePath, String fileName, String destinationFolder) throws IOException {
        assertNotNull(carbonHome, "CARBON_HOME is null");
        String fileAbsPath = filePath + PATH_SEP + fileName;
        log.info("Copying file: " + fileAbsPath);
        File srcFile = new File(fileAbsPath);
        assertTrue(srcFile.exists(), "File does not exist [file] " + srcFile.getAbsolutePath());
        File destFile = new File(carbonHome + PATH_SEP + destinationFolder + PATH_SEP + fileName);
        FileUtils.copyFile(srcFile, destFile);
        log.info("Copying file [source] " + srcFile.getAbsolutePath() + " to [dest] " + destFile.getAbsolutePath());

        // replace placeholders with dynamic values
        String content = IOUtils.toString(new FileInputStream(destFile), StandardCharsets.UTF_8.displayName());
        content = content.replaceAll(Util.ACTIVEMQ_DYNAMIC_PORT_PLACEHOLDER, String.valueOf(activeMQDynamicPort));
        content = content
                .replaceAll(Util.STRATOS_SECURE_DYNAMIC_PORT_PLACEHOLDER, String.valueOf(stratosSecureDynamicPort));
        content = content.replaceAll(Util.STRATOS_DYNAMIC_PORT_PLACEHOLDER, String.valueOf(stratosDynamicPort));
        content = content
                .replaceAll(Util.THRIFT_SECURE_DYNAMIC_PORT_PLACEHOLDER, String.valueOf(thriftSecureDynamicPort));
        content = content.replaceAll(Util.THRIFT_DYNAMIC_PORT_PLACEHOLDER, String.valueOf(thriftDynamicPort));
        IOUtils.write(content, new FileOutputStream(destFile), StandardCharsets.UTF_8.displayName());
    }

    public void setSystemproperties() throws AutomationFrameworkException {
        URL resourceUrl = getClass().getResource(
                File.separator + "keystores" + File.separator + "products" + File.separator + "wso2carbon.jks");
        System.setProperty("javax.net.ssl.trustStore", resourceUrl.getPath());
        System.setProperty("javax.net.ssl.trustStorePassword", "wso2carbon");
        System.setProperty("javax.net.ssl.trustStoreType", "JKS");
        log.info("trustStore set to " + resourceUrl.getPath());

        // Set jndi.properties.dir system property for initializing event receivers
        System.setProperty("jndi.properties.dir", carbonHome + PATH_SEP + Util.CARBON_CONF_PATH);
        try {
            String autoscalerServiceURL = webAppURLHttps + "/services/AutoscalerService";
            System.setProperty(StratosConstants.AUTOSCALER_SERVICE_URL, autoscalerServiceURL);
            log.info("Autoscaler service URL set to " + autoscalerServiceURL);
        } catch (Exception e) {
            throw new AutomationFrameworkException("Could not set autoscaler service URL system property", e);
        }
    }

    public int getActiveMQDynamicPort() {
        return activeMQDynamicPort;
    }

    public void setActiveMQDynamicPort(int activeMQDynamicPort) {
        this.activeMQDynamicPort = activeMQDynamicPort;
    }

    public int getStratosSecureDynamicPort() {
        return stratosSecureDynamicPort;
    }

    public void setStratosSecureDynamicPort(int stratosSecureDynamicPort) {
        this.stratosSecureDynamicPort = stratosSecureDynamicPort;
    }

    public int getStratosDynamicPort() {
        return stratosDynamicPort;
    }

    public void setStratosDynamicPort(int stratosDynamicPort) {
        this.stratosDynamicPort = stratosDynamicPort;
    }

    public int getThriftDynamicPort() {
        return thriftDynamicPort;
    }

    public void setThriftDynamicPort(int thriftDynamicPort) {
        this.thriftDynamicPort = thriftDynamicPort;
    }

    public int getThriftSecureDynamicPort() {
        return thriftSecureDynamicPort;
    }

    public void setThriftSecureDynamicPort(int thriftSecureDynamicPort) {
        this.thriftSecureDynamicPort = thriftSecureDynamicPort;
    }

    public String getWebAppURL() {
        return webAppURL;
    }

    public String getWebAppURLHttps() {
        return webAppURLHttps;
    }

    public void setWebAppURL(String webAppURL) {
        this.webAppURL = webAppURL;
    }

    public void setWebAppURLHttps(String webAppURLHttps) {
        this.webAppURLHttps = webAppURLHttps;
    }
}

// TODO: get rid of this class once startup script issue is fixed in automation engine
class StratosServerManager extends CarbonServerManager {
    private final static Log log = LogFactory.getLog(StratosServerManager.class);
    private Process process;
    private String carbonHome;
    private AutomationContext automationContext;
    private ServerLogReader inputStreamHandler;
    private ServerLogReader errorStreamHandler;
    private boolean isCoverageEnable = false;
    private String coverageDumpFilePath;
    private int portOffset = 0;
    private static final String SERVER_SHUTDOWN_MESSAGE = "Halting JVM";
    private static final String SERVER_STARTUP_MESSAGE = "Mgt Console URL";
    private static final long DEFAULT_START_STOP_WAIT_MS = 300000L;
    private static final String CMD_ARG = "cmdArg";
    private static int defaultHttpPort = Integer.parseInt("9763");
    private static int defaultHttpsPort = Integer.parseInt("9443");
    private static final long RESTART_TIMEOUT = 600000;

    public StratosServerManager(AutomationContext context) {
        super(context);
        this.automationContext = context;
    }

    public synchronized void startServerUsingCarbonHome(String carbonHome, Map<String, String> commandMap)
            throws AutomationFrameworkException {
        if (this.process == null) {
            this.portOffset = this.checkPortAvailability(commandMap);
            Process tempProcess = null;

            try {
                if (!commandMap.isEmpty() && this.getPortOffsetFromCommandMap(commandMap) == 0) {
                    System.setProperty("carbon.home", carbonHome);
                }

                File e = new File(carbonHome);
                log.info("Starting carbon server............. ");
                String scriptName = TestFrameworkUtils.getStartupScriptFileName(carbonHome);
                String[] parameters = this.expandServerStartupCommandList(commandMap);
                String[] cmdArray;
                if (System.getProperty("os.name").toLowerCase().contains("windows")) {
                    e = new File(carbonHome + File.separator + "bin");
                    cmdArray = new String[] { "cmd.exe", "/c", scriptName + ".bat" };
                    cmdArray = this.mergePropertiesToCommandArray(parameters, cmdArray);
                    tempProcess = Runtime.getRuntime().exec(cmdArray, (String[]) null, e);
                } else {
                    cmdArray = new String[] { "sh", "bin/" + scriptName + ".sh" };
                    cmdArray = this.mergePropertiesToCommandArray(parameters, cmdArray);
                    tempProcess = Runtime.getRuntime().exec(cmdArray, (String[]) null, e);
                }

                this.errorStreamHandler = new ServerLogReader("errorStream", tempProcess.getErrorStream());
                this.inputStreamHandler = new ServerLogReader("inputStream", tempProcess.getInputStream());
                this.inputStreamHandler.start();
                this.errorStreamHandler.start();
                Runtime.getRuntime().addShutdownHook(new Thread() {
                    public void run() {
                        try {
                            StratosServerManager.this.serverShutdown(StratosServerManager.this.portOffset);
                        } catch (Exception var2) {
                            log.error("Error while server shutdown ..", var2);
                        }

                    }
                });
                ClientConnectionUtil.waitForPort(defaultHttpPort + this.portOffset, 300000L, false,
                        (String) this.automationContext.getInstance().getHosts().get("default"));
                long time = System.currentTimeMillis() + 60000L;

                while (true) {
                    if (this.inputStreamHandler.getOutput().contains("Mgt Console URL")
                            || System.currentTimeMillis() >= time) {
                        int httpsPort = defaultHttpsPort + this.portOffset;
                        String backendURL = this.automationContext.getContextUrls().getSecureServiceUrl()
                                .replaceAll("(:\\d+)", ":" + httpsPort);
                        User superUser = this.automationContext.getSuperTenant().getTenantAdmin();
                        ClientConnectionUtil.waitForLogin(backendURL, superUser);
                        log.info("Server started successfully.");
                        break;
                    }
                }
            } catch (XPathExpressionException | IOException var13) {
                throw new IllegalStateException("Unable to start server", var13);
            }

            this.process = tempProcess;
        }
    }

    private int checkPortAvailability(Map<String, String> commandMap) throws AutomationFrameworkException {
        int portOffset = this.getPortOffsetFromCommandMap(commandMap);
        if (ClientConnectionUtil.isPortOpen(defaultHttpPort + portOffset)) {
            throw new AutomationFrameworkException(
                    "Unable to start carbon server on port " + (defaultHttpPort + portOffset) +
                            " : Port already in use");
        } else if (ClientConnectionUtil.isPortOpen(defaultHttpsPort + portOffset)) {
            throw new AutomationFrameworkException(
                    "Unable to start carbon server on port " + (defaultHttpsPort + portOffset) +
                            " : Port already in use");
        } else {
            return portOffset;
        }
    }

    private String[] mergePropertiesToCommandArray(String[] parameters, String[] cmdArray) {
        if (parameters != null) {
            cmdArray = this.mergerArrays(cmdArray, parameters);
        }

        return cmdArray;
    }

    public synchronized String setUpCarbonHome(String carbonServerZipFile)
            throws IOException, AutomationFrameworkException {
        if (this.process != null) {
            return this.carbonHome;
        } else {
            int indexOfZip = carbonServerZipFile.lastIndexOf(".zip");
            if (indexOfZip == -1) {
                throw new IllegalArgumentException(carbonServerZipFile + " is not a zip file");
            } else {
                String fileSeparator = File.separator.equals("\\") ? "\\" : "/";
                if (fileSeparator.equals("\\")) {
                    carbonServerZipFile = carbonServerZipFile.replace("/", "\\");
                }

                String extractedCarbonDir = carbonServerZipFile
                        .substring(carbonServerZipFile.lastIndexOf(fileSeparator) + 1, indexOfZip);
                FileManipulator.deleteDir(extractedCarbonDir);
                String extractDir = "carbontmp" + System.currentTimeMillis();
                String baseDir = System.getProperty("basedir", ".") + File.separator + "target";
                log.info("Extracting carbon zip file.. ");
                (new ArchiveExtractor()).extractFile(carbonServerZipFile, baseDir + File.separator + extractDir);
                this.carbonHome = (new File(baseDir)).getAbsolutePath() + File.separator + extractDir + File.separator +
                        extractedCarbonDir;

                try {
                    this.isCoverageEnable = Boolean
                            .parseBoolean(this.automationContext.getConfigurationValue("//coverage"));
                } catch (XPathExpressionException var8) {
                    throw new AutomationFrameworkException("Coverage configuration not found in automation.xml", var8);
                }
                // Fix startup script issue by copying stratos.sh as stratos-server.sh
                // TODO: remove this class after automation engine provides a way to pass startup script name
                // currently startup script should be either wso2server.sh or contain the string 'server'
                FileUtils.copyFile(new File(carbonHome + File.separator + "bin" + File.separator + "stratos.sh"),
                        new File(carbonHome + File.separator + "bin" + File.separator + "stratos-server.sh"));

                if (this.isCoverageEnable) {
                    this.instrumentForCoverage();
                }

                return this.carbonHome;
            }
        }
    }

    public synchronized void serverShutdown(int portOffset) throws AutomationFrameworkException {
        if (this.process != null) {
            log.info("Shutting down server..");
            if (ClientConnectionUtil.isPortOpen(Integer.parseInt("9443") + portOffset)) {
                int e = defaultHttpsPort + portOffset;
                String url = null;

                try {
                    url = this.automationContext.getContextUrls().getBackEndUrl();
                } catch (XPathExpressionException var10) {
                    throw new AutomationFrameworkException("Get context failed", var10);
                }

                String backendURL = url.replaceAll("(:\\d+)", ":" + e);

                try {
                    ClientConnectionUtil.sendForcefulShutDownRequest(backendURL,
                            this.automationContext.getSuperTenant().getContextUser().getUserName(),
                            this.automationContext.getSuperTenant().getContextUser().getPassword());
                } catch (AutomationFrameworkException var8) {
                    throw new AutomationFrameworkException("Get context failed", var8);
                } catch (XPathExpressionException var9) {
                    throw new AutomationFrameworkException("Get context failed", var9);
                }

                long time = System.currentTimeMillis() + 300000L;

                while (!this.inputStreamHandler.getOutput().contains("Halting JVM")
                        && System.currentTimeMillis() < time) {
                    ;
                }

                log.info("Server stopped successfully...");
            }

            this.inputStreamHandler.stop();
            this.errorStreamHandler.stop();
            this.process.destroy();
            this.process = null;
            if (this.isCoverageEnable) {
                try {
                    log.info("Generating Jacoco code coverage...");
                    this.generateCoverageReport(
                            new File(this.carbonHome + File.separator + "repository" + File.separator + "components" +
                                    File.separator + "plugins" + File.separator));
                } catch (IOException var7) {
                    log.error("Failed to generate code coverage ", var7);
                    throw new AutomationFrameworkException("Failed to generate code coverage ", var7);
                }
            }

            if (portOffset == 0) {
                System.clearProperty("carbon.home");
            }
        }

    }

    private void generateCoverageReport(File classesDir) throws IOException, AutomationFrameworkException {
        CodeCoverageUtils
                .executeMerge(FrameworkPathUtil.getJacocoCoverageHome(), FrameworkPathUtil.getCoverageMergeFilePath());
        ReportGenerator reportGenerator = new ReportGenerator(new File(FrameworkPathUtil.getCoverageMergeFilePath()),
                classesDir, new File(CodeCoverageUtils.getJacocoReportDirectory()), (File) null);
        reportGenerator.create();
        log.info("Jacoco coverage dump file path : " + FrameworkPathUtil.getCoverageDumpFilePath());
        log.info("Jacoco class file path : " + classesDir);
        log.info("Jacoco coverage HTML report path : " + CodeCoverageUtils.getJacocoReportDirectory() +
                File.separator + "index.html");
    }

    public synchronized void restartGracefully() throws AutomationFrameworkException {
        try {
            int time = defaultHttpsPort + this.portOffset;
            String backendURL = this.automationContext.getContextUrls().getSecureServiceUrl()
                    .replaceAll("(:\\d+)", ":" + time);
            User e = this.automationContext.getSuperTenant().getTenantAdmin();
            ClientConnectionUtil.sendGraceFullRestartRequest(backendURL, e.getUserName(), e.getPassword());
        } catch (XPathExpressionException var5) {
            throw new AutomationFrameworkException("restart failed", var5);
        }

        long time1 = System.currentTimeMillis() + 300000L;

        while (!this.inputStreamHandler.getOutput().contains("Halting JVM") && System.currentTimeMillis() < time1) {
            ;
        }

        time1 = System.currentTimeMillis();

        while (System.currentTimeMillis() < time1 + 5000L) {
            ;
        }

        try {
            ClientConnectionUtil.waitForPort(
                    Integer.parseInt((String) this.automationContext.getInstance().getPorts().get("https")),
                    RESTART_TIMEOUT, true, (String) this.automationContext.getInstance().getHosts().get("default"));
            ClientConnectionUtil.waitForLogin(this.automationContext);
        } catch (XPathExpressionException var4) {
            throw new AutomationFrameworkException("Connection attempt to carbon server failed", var4);
        }
    }

    private String[] expandServerStartupCommandList(Map<String, String> commandMap) {
        if (commandMap != null && commandMap.size() != 0) {
            String[] cmdParaArray = null;
            String cmdArg = null;
            if (commandMap.containsKey("cmdArg")) {
                cmdArg = (String) commandMap.get("cmdArg");
                cmdParaArray = cmdArg.trim().split("\\s+");
                commandMap.remove("cmdArg");
            }

            String[] parameterArray = new String[commandMap.size()];
            int arrayIndex = 0;
            Set entries = commandMap.entrySet();

            String parameter;
            for (Iterator i$ = entries.iterator(); i$.hasNext(); parameterArray[arrayIndex++] = parameter) {
                Map.Entry entry = (Map.Entry) i$.next();
                String key = (String) entry.getKey();
                String value = (String) entry.getValue();
                if (value != null && !value.isEmpty()) {
                    parameter = key + "=" + value;
                } else {
                    parameter = key;
                }
            }

            if (cmdArg != null) {
                commandMap.put("cmdArg", cmdArg);
            }

            if (cmdParaArray != null && cmdParaArray.length != 0) {
                return (String[]) ArrayUtils.addAll(parameterArray, cmdParaArray);
            } else {
                return parameterArray;
            }
        } else {
            return null;
        }
    }

    private int getPortOffsetFromCommandMap(Map<String, String> commandMap) {
        return commandMap.containsKey("-DportOffset") ? Integer.parseInt((String) commandMap.get("-DportOffset")) : 0;
    }

    private String[] mergerArrays(String[] array1, String[] array2) {
        return (String[]) ArrayUtils.addAll(array1, array2);
    }

    private void insertJacocoAgentToShellScript(String scriptName) throws IOException {
        String jacocoAgentFile = CodeCoverageUtils.getJacocoAgentJarLocation();
        this.coverageDumpFilePath = FrameworkPathUtil.getCoverageDumpFilePath();
        CodeCoverageUtils.insertStringToFile(
                new File(this.carbonHome + File.separator + "bin" + File.separator + scriptName + ".sh"),
                new File(this.carbonHome + File.separator + "tmp" + File.separator + scriptName + ".sh"),
                "-Dwso2.server.standalone=true",
                "-javaagent:" + jacocoAgentFile + "=destfile=" + this.coverageDumpFilePath + "" +
                        ",append=true,includes=" + CodeCoverageUtils.getInclusionJarsPattern(":") + " \\");
    }

    private void insertJacocoAgentToBatScript(String scriptName) throws IOException {
        String jacocoAgentFile = CodeCoverageUtils.getJacocoAgentJarLocation();
        this.coverageDumpFilePath = FrameworkPathUtil.getCoverageDumpFilePath();
        CodeCoverageUtils.insertJacocoAgentToStartupBat(
                new File(this.carbonHome + File.separator + "bin" + File.separator + scriptName + ".bat"),
                new File(this.carbonHome + File.separator + "tmp" + File.separator + scriptName + ".bat"),
                "-Dcatalina.base", "-javaagent:" + jacocoAgentFile + "=destfile=" + this.coverageDumpFilePath + "" +
                        ",append=true,includes=" + CodeCoverageUtils.getInclusionJarsPattern(":"));
    }

    private void instrumentForCoverage() throws IOException, AutomationFrameworkException {
        String scriptName = TestFrameworkUtils.getStartupScriptFileName(this.carbonHome);
        if (System.getProperty("os.name").toLowerCase().contains("windows")) {
            this.insertJacocoAgentToBatScript(scriptName);
            if (log.isDebugEnabled()) {
                log.debug("Included files " + CodeCoverageUtils.getInclusionJarsPattern(":"));
                log.debug("Excluded files " + CodeCoverageUtils.getExclusionJarsPattern(":"));
            }
        } else {
            this.insertJacocoAgentToShellScript(scriptName);
        }

    }
}
