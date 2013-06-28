package org.wso2.carbon.cartridge.messages;

import org.apache.axis2.clustering.ClusteringCommand;
import org.apache.axis2.clustering.ClusteringFault;
import org.apache.axis2.clustering.ClusteringMessage;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class CreateRemoveClusterDomainMessage extends ClusteringMessage{
	
	private static final Log log = LogFactory.getLog(CreateClusterDomainMessage.class);
	public static final String CLUSTER_DOMAIN_MANAGER = "cluster.domain.manager";
	private String domain;
	private String subDomain;
	private String hostName;
	
	
	
	public CreateRemoveClusterDomainMessage(String domain, String subDomain, String hostName) {
	    this.domain = domain;
	    this.subDomain = subDomain;
	    this.hostName = hostName;
    }

	@Override
    public ClusteringCommand getResponse() {
		return new ClusteringCommand() {
            @Override
            public void execute(ConfigurationContext configurationContext) throws ClusteringFault {
                log.info("Received response to CreateRemoveClusterDomainMessage");
            }
        };
    }

	@Override
    public void execute(final ConfigurationContext configurationContext) throws ClusteringFault {
	    
		log.info("Received ***" + this);
        Runnable runnable = new Runnable() {
            public void run() {
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException ignored) {
                }
                ClusterDomainManager clusterDomainManager =
                        (ClusterDomainManager) configurationContext.getProperty(CLUSTER_DOMAIN_MANAGER);
                log.info(" ***** Received clusterDomain Manager ** " + clusterDomainManager);
                if (clusterDomainManager != null) {
                    clusterDomainManager.removeClusterDomain(domain, subDomain, hostName);
                } else {
                    log.warn(CLUSTER_DOMAIN_MANAGER + " has not been defined in ConfigurationContext");
                }
            }
        };
        new Thread(runnable).start();
    }

}
