/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.stratos.adc.mgt.instance.utils;

public class CartridgeInstanceUtils {
    /*
    private static Log log = LogFactory.getLog(CartridgeInstanceUtils.class);

    public static void validateCartridgeAlias(String alias, String cartridgeType) throws InvalidCartridgeAliasException,
            DuplicateCartridgeAliasException, ADCException {

        // Do not use quotes in messages, current UI JavaScript does not work if there are quotes
        // TODO: Fix message display in UI
        String patternString = "([a-z0-9]+([-][a-z0-9])*)+";
        Pattern pattern = Pattern.compile(patternString);

        if (!pattern.matcher(alias).matches()) {
            String msg = "The alias " + alias + " can contain only alpha-numeric lowercase characters. Please enter a valid alias.";
            log.error(msg);
            throw new InvalidCartridgeAliasException(msg, cartridgeType, alias);
        }

        boolean isAliasTaken = false;
        try {
            isAliasTaken = PersistenceManager.isAliasAlreadyTaken(alias, cartridgeType);
        } catch (Exception e) {
            String msg = "Exception : " + e.getMessage();
            log.error(msg, e);
            throw new ADCException("Error when checking alias is already taken", e);
        }

        if (isAliasTaken) {
            String msg = "The alias " + alias + " is already taken. Please try again with a different alias.";
            log.error(msg);
            throw new DuplicateCartridgeAliasException(msg, cartridgeType, alias);
        }
    }

    public static String generatePassword() {

        final int PASSWORD_LENGTH = 8;
        StringBuffer sb = new StringBuffer();
        for (int x = 0; x < PASSWORD_LENGTH; x++) {
            sb.append((char) ((int) (Math.random() * 26) + 97));
        }
        return sb.toString();

    }

    public static CartridgeSubscription createCartridgeSubscription(CartridgeInfo cartridgeInfo,
                                                                     Policy policy,
                                                                     String cartridgeType,
                                                                     String cartridgeAlias,
                                                                     int tenantId,
                                                                     String tenantDomain,
                                                                     Repository repository,
                                                                     String clusterDomain,
                                                                     String clusterSubDomain,
                                                                     String hostName,
                                                                     String mgtClusterDomain,
                                                                     String mgtClusterSubDomain,
                                                                     DataCartridge dataCartridge) {

        CartridgeSubscription cartridgeSubscription = new CartridgeSubscription();
        cartridgeSubscription.setCartridge(cartridgeType);
        cartridgeSubscription.setAlias(cartridgeAlias);
        cartridgeSubscription.setClusterDomain(clusterDomain);
        cartridgeSubscription.setClusterSubdomain(clusterSubDomain);
        cartridgeSubscription.setMgtClusterDomain(mgtClusterDomain);
        cartridgeSubscription.setMgtClusterSubDomain(mgtClusterSubDomain);
        cartridgeSubscription.setHostName(hostName);
        cartridgeSubscription.setPolicy(policy.getName());
        cartridgeSubscription.setRepository(repository);
        cartridgeSubscription.setPortMappings(createPortMappings(cartridgeInfo));
        cartridgeSubscription.setProvider(cartridgeInfo.getProvider());
        cartridgeSubscription.setDataCartridge(dataCartridge);
        cartridgeSubscription.setTenantId(tenantId);
        cartridgeSubscription.setTenantDomain(tenantDomain);
        cartridgeSubscription.setBaseDirectory(cartridgeInfo.getBaseDir());
        cartridgeSubscription.setState("PENDING");

        return cartridgeSubscription;
    }

    private static List<PortMapping> createPortMappings(CartridgeInfo cartridgeInfo) {
        List<PortMapping> portMappings = new ArrayList<PortMapping>();

        if (cartridgeInfo.getPortMappings() != null) {
            for (org.apache.stratos.cloud.controller.util.xsd.PortMapping portMapping : cartridgeInfo.getPortMappings()) {
                PortMapping portMap = new PortMapping();
                portMap.setPrimaryPort(portMapping.getPort());
                portMap.setProxyPort(portMapping.getProxyPort());
                portMap.setType(portMapping.getProtocol());
                portMappings.add(portMap);
            }
        }
        return portMappings;
    }

    public static void addDNSEntry(String alias, String cartridgeType) {
        new DNSManager().addNewSubDomain(alias + "." + cartridgeType, System.getProperty(CartridgeConstants.ELB_IP));
    }

    public static SubscriptionInfo createSubscriptionResponse(CartridgeSubscription cartridgeSubscription, Repository repository) {
        SubscriptionInfo subscriptionInfo = new SubscriptionInfo();

        if (repository != null && repository.getUrl() != null) {
            subscriptionInfo.setRepositoryURL(convertRepoURL(repository.getUrl()));
        }
        subscriptionInfo.setHostname(cartridgeSubscription.getHostName());

        return subscriptionInfo;
    }

    private static String convertRepoURL(String gitURL) {
        String convertedHttpURL = null;
        if (gitURL != null && gitURL.startsWith("git@")) {
            StringBuilder httpRepoUrl = new StringBuilder();
            httpRepoUrl.append("http://");
            String[] urls = gitURL.split(":");
            String[] hostNameArray = urls[0].split("@");
            String hostName = hostNameArray[1];
            httpRepoUrl.append(hostName).append("/").append(urls[1]);
            convertedHttpURL = httpRepoUrl.toString();
        } else if (gitURL != null && gitURL.startsWith("http")) {
            convertedHttpURL = gitURL;
        }
        return convertedHttpURL;
    }

    public static RepositoryInformation validateRepository(String repoURL, String repoUsername, String repoPassword,
                                                           boolean privateRepo, boolean testConnection)

            throws RepositoryRequiredException, ADCException,
            RepositoryCredentialsRequiredException, InvalidRepositoryException, RepositoryTransportException {

        RepositoryInformation repositoryInformation = new RepositoryInformation();
        repositoryInformation.setRepoURL(repoURL);
        if (log.isDebugEnabled()) {
            log.debug("Validating Git Repository");
        }

        if (repoURL == null || repoURL.trim().length() == 0) {
            throw new RepositoryRequiredException("External repository required for subscription");
        }

        if (repoURL != null && repoURL.trim().length() > 0 && privateRepo) {
            if (log.isDebugEnabled()) {
                log.debug("External repo validation is a private repo: " + repoURL);
            }
            if (repoUsername == null || repoUsername.trim().length() == 0 || repoPassword == null
                    || repoPassword.trim().length() == 0) {
                throw new RepositoryCredentialsRequiredException(
                        "Username and Password are required for private repository");
            }
        }

        if (!testConnection) {
            if (log.isDebugEnabled()) {
                log.debug("External repo validation is not enabled");
            }
            return repositoryInformation;
        }

        if (repoURL == null || repoURL.trim().length() == 0) {
            // This means, no repo to validate.
            return repositoryInformation;
        }

        if (log.isDebugEnabled()) {
            log.debug("External repo validation enabled");
        }

        // This assumes running on Linux.
        String parentDirName = "/tmp/" + UUID.randomUUID().toString();
        CredentialsProvider credentialsProvider = null;
        if (repoUsername != null && repoUsername.trim().length() > 0 && repoPassword != null
                && repoPassword.trim().length() > 0) {
            if (log.isDebugEnabled()) {
                log.debug("External repo credentails are passed: " + repoUsername);
            }
            credentialsProvider = new UsernamePasswordCredentialsProvider(repoUsername, repoPassword.toCharArray());
        }

        // Initialize temp local file repo
        FileRepository localRepo = null;
        try {
            File f = new File(parentDirName + "/.git");
            localRepo = new FileRepository(f);
            if (log.isDebugEnabled()) {
                log.debug("Local File Repo: " + f.getAbsoluteFile());
            }
        } catch (IOException e) {
            throw new ADCException("Error creating local file repo", e);
        }

        Git git = new Git(localRepo);
        LsRemoteCommand cmd = git.lsRemote().setRemote(repoURL);
        if (credentialsProvider != null) {
            cmd.setCredentialsProvider(credentialsProvider);
        }
        try {
            Collection<Ref> collection = cmd.call();
            List<String> refNames = new ArrayList<String>();
            if (collection != null) {
                for (Ref ref : collection) {
                    if (log.isDebugEnabled()) {
                        log.debug(repoURL + ": " + ref.getName());
                    }
                    refNames.add(ref.getName());
                }
            }
            repositoryInformation.setRefName(refNames.toArray(new String[refNames.size()]));
        } catch (InvalidRemoteException e) {
            throw new InvalidRepositoryException("Provided repository url is not valid", e);
        } catch (TransportException e) {
            throw new RepositoryTransportException("Transport error when checking remote repository", e);
        } catch (GitAPIException e) {
            throw new ADCException("Git API error when checking remote repository", e);
        }
        return repositoryInformation;
    }*/
}
