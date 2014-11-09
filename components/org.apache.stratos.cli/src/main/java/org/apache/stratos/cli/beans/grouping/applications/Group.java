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
package org.apache.stratos.cli.beans.grouping.applications;

public class Group
{
    private String deploymentPolicy;

    private Cluster cluster;

    private String alias;

    private String autoScalingPolicy;

    private SubGroup[] subGroups;

    public String getDeploymentPolicy ()
    {
        return deploymentPolicy;
    }

    public void setDeploymentPolicy (String deploymentPolicy)
    {
        this.deploymentPolicy = deploymentPolicy;
    }

    public Cluster getCluster()
    {
        return cluster;
    }

    public void setCluster(Cluster cluster)
    {
        this.cluster = cluster;
    }

    public String getAlias ()
    {
        return alias;
    }

    public void setAlias (String alias)
    {
        this.alias = alias;
    }

    public String getAutoScalingPolicy ()
    {
        return autoScalingPolicy;
    }

    public void setAutoScalingPolicy (String autoScalingPolicy)
    {
        this.autoScalingPolicy = autoScalingPolicy;
    }

    public SubGroup[] getSubGroups ()
    {
        return subGroups;
    }

    public void setSubGroups (SubGroup[] subGroups)
    {
        this.subGroups = subGroups;
    }
}
