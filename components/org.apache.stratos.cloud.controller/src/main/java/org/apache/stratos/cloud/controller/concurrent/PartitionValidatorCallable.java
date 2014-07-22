/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.stratos.cloud.controller.concurrent;

import java.util.concurrent.Callable;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.cloud.controller.deployment.partition.Partition;
import org.apache.stratos.cloud.controller.exception.InvalidIaasProviderException;
import org.apache.stratos.cloud.controller.exception.InvalidPartitionException;
import org.apache.stratos.cloud.controller.interfaces.Iaas;
import org.apache.stratos.cloud.controller.pojo.Cartridge;
import org.apache.stratos.cloud.controller.pojo.IaasProvider;
import org.apache.stratos.cloud.controller.util.CloudControllerUtil;
import org.apache.stratos.cloud.controller.validate.interfaces.PartitionValidator;

public class PartitionValidatorCallable implements Callable<IaasProvider> {
	
	private static final Log log = LogFactory.getLog(PartitionValidatorCallable.class);
	private Partition partition;
	private Cartridge cartridge;
	
	public PartitionValidatorCallable(Partition partition, Cartridge cartridge) {
		this.partition = partition;
		this.cartridge = cartridge;
	}

	@Override
	public IaasProvider call() throws Exception {
		
		if (log.isDebugEnabled()) {
			log.debug("Partition validation started for "+partition+" of "+cartridge);
		}
		String provider = partition.getProvider();
        IaasProvider iaasProvider = cartridge.getIaasProvider(provider);

        if (iaasProvider == null) {
            String msg =
                         "Invalid Partition - " + partition.toString() +
                                 ". Cause: Iaas Provider is null for Provider: " + provider;
            log.error(msg);
            throw new InvalidPartitionException(msg);
        }

        Iaas iaas = iaasProvider.getIaas();
        
        if (iaas == null) {
            
            try {
                iaas = CloudControllerUtil.getIaas(iaasProvider);
            } catch (InvalidIaasProviderException e) {
                String msg =
                        "Invalid Partition - " + partition.toString() +
                        ". Cause: Unable to build Iaas of this IaasProvider [Provider] : " + provider+". "+e.getMessage();
                log.error(msg, e);
                throw new InvalidPartitionException(msg, e);
            }
            
        }
        
        PartitionValidator validator = iaas.getPartitionValidator();
        validator.setIaasProvider(iaasProvider);
        IaasProvider updatedIaasProvider =
                                           validator.validate(partition.getId(),
                                                              CloudControllerUtil.toJavaUtilProperties(partition.getProperties()));
        
        if (log.isDebugEnabled()) {
        	log.debug("Partition "+partition.toString()+ " is validated successfully "
        			+ "against the Cartridge: "+cartridge.getType());
        }
        
        return updatedIaasProvider;
	}

}
