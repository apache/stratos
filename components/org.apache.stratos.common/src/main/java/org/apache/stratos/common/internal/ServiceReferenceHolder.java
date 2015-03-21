package org.apache.stratos.common.internal;

import com.hazelcast.core.HazelcastInstance;
import org.apache.axis2.engine.AxisConfiguration;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.common.services.DistributedObjectProvider;
import org.wso2.carbon.caching.impl.DistributedMapProvider;
import org.wso2.carbon.registry.core.service.RegistryService;
import org.wso2.carbon.user.core.service.RealmService;

/**
 * Stratos common service reference holder.
 */
public class ServiceReferenceHolder {

    private static final Log log = LogFactory.getLog(ServiceReferenceHolder.class);

    private static volatile ServiceReferenceHolder instance;
    private HazelcastInstance hazelcastInstance;
    private DistributedMapProvider distributedMapProvider;
    private RealmService realmService;
    private RegistryService registryService;
    private AxisConfiguration axisConfiguration;
    private DistributedObjectProvider distributedObjectProvider;

    private ServiceReferenceHolder() {
    }

    public static ServiceReferenceHolder getInstance() {
        if (instance == null) {
            synchronized (ServiceReferenceHolder.class) {
                if (instance == null) {
                    instance = new ServiceReferenceHolder();
                }
            }
        }
        return instance;
    }

    public void setHazelcastInstance(HazelcastInstance hazelcastInstance) {
        this.hazelcastInstance = hazelcastInstance;
    }

    public HazelcastInstance getHazelcastInstance() {
        return hazelcastInstance;
    }


    public void setDistributedMapProvider(DistributedMapProvider distributedMapProvider) {
        this.distributedMapProvider = distributedMapProvider;
    }

    public DistributedMapProvider getDistributedMapProvider() {
        return distributedMapProvider;
    }

    public void setRealmService(RealmService realmService) {
        this.realmService = realmService;
    }

    public RealmService getRealmService() {
        return realmService;
    }

    public void setRegistryService(RegistryService registryService) {
        this.registryService = registryService;
    }

    public RegistryService getRegistryService() {
        return registryService;
    }

    public void setAxisConfiguration(AxisConfiguration axisConfiguration) {
        this.axisConfiguration = axisConfiguration;
    }

    public AxisConfiguration getAxisConfiguration() {
        return axisConfiguration;
    }

    public void setDistributedObjectProvider(DistributedObjectProvider distributedObjectProvider) {
        this.distributedObjectProvider = distributedObjectProvider;
    }

    public DistributedObjectProvider getDistributedObjectProvider() {
        return distributedObjectProvider;
    }
}
