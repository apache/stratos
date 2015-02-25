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
package org.apache.stratos.autoscaler.internal;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.autoscaler.commands.ASPolicyCommands;
import org.eclipse.osgi.framework.console.CommandProvider;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

public class ASBundleActivater implements BundleActivator{

    private static final Log log = LogFactory.getLog(ASBundleActivater.class);
  
    @Override
	public void start(BundleContext context) throws Exception {
    	if(log.isDebugEnabled())
    		log.debug("AutoScaler bundle is activated.");
    	
		context.registerService(CommandProvider.class.getName(),new ASPolicyCommands(), null);
	}

	@Override
	public void stop(BundleContext context) throws Exception {
		if(log.isDebugEnabled())
    		log.debug("AutoScaler bundle is deActivated.");		
	}
}