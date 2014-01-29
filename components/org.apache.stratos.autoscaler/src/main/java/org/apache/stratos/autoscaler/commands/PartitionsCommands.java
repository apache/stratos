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

package org.apache.stratos.autoscaler.commands;

import org.apache.commons.lang.StringUtils;
import org.apache.stratos.autoscaler.partition.PartitionManager;
import org.apache.stratos.cloud.controller.deployment.partition.Partition;
import org.eclipse.osgi.framework.console.CommandInterpreter;
import org.eclipse.osgi.framework.console.CommandProvider;
 
public class PartitionsCommands implements CommandProvider{
 
    public String getHelp() {
        return "\nlistPartitions - List partitions deployed to AutoScaler. \n"
        		+ "\t parameters : \n"
        		+ "\t\t String   partitionID : ID of the partition.\n";
    }
 
    public void _listPartitions (CommandInterpreter ci){
    	String partitionId = ci.nextArgument();
    	
    	PartitionManager pm = PartitionManager.getInstance();
    	
    	if(StringUtils.isBlank(partitionId)){
    		Partition[] partitionArr = pm.getAllPartitions();
        	for(Partition partition : partitionArr){
        		ci.println(partition.toString());
        	}
    	}else{
    		Partition partition = pm.getPartitionById(partitionId);
    		if(partition != null){	
    			ci.println(partition);
    		}
    	}
    }
}