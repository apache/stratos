package org.apache.stratos.cloud.controller.validate;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.cloud.controller.exception.InvalidPartitionException;
import org.apache.stratos.cloud.controller.interfaces.Iaas;
import org.apache.stratos.cloud.controller.pojo.IaasProvider;
import org.apache.stratos.cloud.controller.validate.interfaces.PartitionValidator;
import org.apache.stratos.messaging.domain.topology.Scope;

import java.util.Properties;

/**
 * Created by sanjaya on 9/11/14.
 */
public class CloudstackPartitionValidator implements PartitionValidator {


    private static final Log log = LogFactory.getLog(AWSEC2PartitionValidator.class);
    private IaasProvider iaasProvider;
    private Iaas iaas;


    @Override
    public void setIaasProvider(IaasProvider iaas) {
        this.iaasProvider = iaas;
        this.iaas = iaas.getIaas();
    }

    @Override
    public IaasProvider validate(String partitionId, Properties properties) throws InvalidPartitionException {



        return iaasProvider;
    }
}
