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
package org.apache.stratos.autoscaler.applications.dependency;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.autoscaler.applications.dependency.context.ApplicationChildContext;
import org.apache.stratos.autoscaler.monitor.Monitor;
import org.apache.stratos.autoscaler.monitor.component.ParentComponentMonitor;

import java.util.ArrayList;
import java.util.List;

/**
 * This is to contain the dependency tree of an application/group
 */
public class DependencyTree {

    private static final Log log = LogFactory.getLog(DependencyTree.class);

    private List<ApplicationChildContext> primaryApplicationContextList;
    private TerminationBehavior terminationBehavior;
    private String id;

    public DependencyTree(String id) {
        primaryApplicationContextList = new ArrayList<ApplicationChildContext>();
        this.setId(id);
        if (log.isDebugEnabled()) {
            log.debug("Starting a dependency tree for the [group/application] " + id);
        }
    }

    public List<ApplicationChildContext> getPrimaryApplicationContextList() {
        return primaryApplicationContextList;
    }

    public void setPrimaryApplicationContextList(List<ApplicationChildContext> primaryApplicationContextList) {
        this.primaryApplicationContextList = primaryApplicationContextList;
    }

    public void addPrimaryApplicationContext(ApplicationChildContext applicationContext) {
        primaryApplicationContextList.add(applicationContext);

    }

    /**
     * Find an ApplicationContext from dependency tree with the given id
     *
     * @param id the alias/id of group/cluster
     * @return ApplicationContext of the given id
     */
    public ApplicationChildContext getApplicationChildContextByIdInPrimaryTree(String id) {
        return getApplicationChildContextById(id, primaryApplicationContextList);
    }

    /**
     * Find the ApplicationContext using Breadth first search.
     *
     * @param id       the alias/id of group/cluster
     * @param contexts the list of contexts in the same level of the tree
     * @return ApplicationContext of the given id
     */
    private ApplicationChildContext getApplicationChildContextById(String id, List<ApplicationChildContext> contexts) {
        for (ApplicationChildContext context : contexts) {
            //TODO check for the status
            if (context.getId().equals(id)) {
                return context;
            }
        }
        //if not found in the top level search recursively
        for (ApplicationChildContext context : contexts) {
            return getApplicationChildContextById(id, context.getApplicationChildContextList());
        }
        return null;
    }

    public ApplicationChildContext findParentContextWithId(String id) {
        return findParentContextWithId(null, id, this.primaryApplicationContextList);
    }

    public List<ApplicationChildContext> findAllParentContextWithId(String id) {
        List<ApplicationChildContext> applicationContexts = new ArrayList<ApplicationChildContext>();
        return findAllParent(applicationContexts, id);
    }

    private List<ApplicationChildContext> findAllParent(List<ApplicationChildContext> parentContexts, String id) {
        ApplicationChildContext context = findParentContextWithId(null, id, this.primaryApplicationContextList);
        if (context != null) {
            parentContexts.add(context);
            findAllParent(parentContexts, context.getId());
        }
        return parentContexts;
    }


    private ApplicationChildContext findParentContextWithId(ApplicationChildContext parent, String id,
                                                            List<ApplicationChildContext> contexts) {
        for (ApplicationChildContext context : contexts) {
            //TODO check for the status
            if (context.getParent() != null && context.getParent().equals(id)) {
                return context;
            }
        }
        //if not found in the top level search recursively
        for (ApplicationChildContext context : this.primaryApplicationContextList) {
            if(context.getApplicationChildContextList() != null &&
                    !context.getApplicationChildContextList().isEmpty()) {
                return findParentContextWithId(context, id, context.getApplicationChildContextList());
            }
        }
        return null;
    }

    /**
     * Getting the next start able dependencies upon the activate event
     * received for a group/cluster which is part of this tree.
     *
     * @param id the alias/id of group/cluster which received the activated event.
     * @return list of dependencies
     */
    public List<ApplicationChildContext> getStarAbleDependencies(String id) {
        //finding the application context which received the activated event and
        // returning it's immediate children as the dependencies.
        ApplicationChildContext context = getApplicationChildContextByIdInPrimaryTree(id);
        return context.getApplicationChildContextList();
    }

    /**
     * Getting the next start able dependencies upon the monitor initialization.
     *
     * @return list of dependencies
     */
    public List<ApplicationChildContext> getStartAbleDependencies() {
        //returning the top level as the monitor is in initializing state
        return this.primaryApplicationContextList;
    }

    public List<ApplicationChildContext> getStarAbleDependenciesByTermination(
            ParentComponentMonitor monitor,
            String instanceId) {
        //Breadth First search over the graph to find out which level has the terminated contexts

        return traverseGraphByLevel(this.primaryApplicationContextList, monitor, instanceId);
    }


    private List<ApplicationChildContext> traverseGraphByLevel(List<ApplicationChildContext> contexts,
                                                               ParentComponentMonitor parentMonitor,
                                                               String instanceId) {
        for (ApplicationChildContext context : contexts) {
            Monitor monitor = parentMonitor.getMonitor(context.getId());
            if (monitor.getInstance(instanceId) == null ||
                    monitor.getInstancesByParentInstanceId(instanceId).isEmpty()) {
                return contexts;
            }
        }

        for (ApplicationChildContext context : contexts) {
            return traverseGraphByLevel(context.getApplicationChildContextList(), parentMonitor, instanceId);
        }
        return null;
    }


    /**
     * When one group/cluster terminates/in_maintenance, need to consider about other
     * dependencies
     *
     * @param id the alias/id of group/cluster in which terminated event received
     * @return all the kill able children dependencies
     */
    public List<ApplicationChildContext> getTerminationDependencies(String id) {
        List<ApplicationChildContext> allChildrenOfAppContext = new ArrayList<ApplicationChildContext>();
        ApplicationChildContext applicationContext = getApplicationChildContextByIdInPrimaryTree(id);
        if (getTerminationBehavior() == TerminationBehavior.TERMINATE_DEPENDENT) {
            //finding the ApplicationContext of the given id
            //finding all the children of the found application context
            findAllChildrenOfAppContext(applicationContext.getApplicationChildContextList(),
                    allChildrenOfAppContext);
            //If only particular cluster, then no need to terminated it.
            if (allChildrenOfAppContext.size() > 0) {
                //adding the terminated one to the list
                allChildrenOfAppContext.add(applicationContext);
            }
            return allChildrenOfAppContext;
        } else if (getTerminationBehavior() == TerminationBehavior.TERMINATE_ALL) {
            //killall will be killed by the monitor from it's list.
            findAllChildrenOfAppContext(this.primaryApplicationContextList,
                    allChildrenOfAppContext);

        }
        //return empty for the kill-none case, what ever returns here will be killed in
        return allChildrenOfAppContext;
    }

    public List<ApplicationChildContext> getScalingDependencies(String id) {
        return null;
    }

    /**
     * This will help to find out all the children of a particular node
     *
     * @param applicationContexts app contexts of the particular node
     * @param childContexts       contains the children of the node
     * @return all the children of the given node
     */
    public List<ApplicationChildContext> findAllChildrenOfAppContext(List<ApplicationChildContext> applicationContexts,
                                                                     List<ApplicationChildContext> childContexts) {
        for (ApplicationChildContext context : applicationContexts) {
            childContexts.add(context);
            findAllChildrenOfAppContext(context.getApplicationChildContextList(), childContexts);
        }
        return childContexts;
    }

    public void setTerminationBehavior(TerminationBehavior terminationBehavior) {
        this.terminationBehavior = terminationBehavior;
    }

    public boolean isTerminateDependent() {
        return this.getTerminationBehavior() == TerminationBehavior.TERMINATE_DEPENDENT;
    }

    public boolean isTerminateAll() {
        return this.getTerminationBehavior() == TerminationBehavior.TERMINATE_ALL;
    }


    public TerminationBehavior getTerminationBehavior() {
        return terminationBehavior;
    }

    public enum TerminationBehavior {
        TERMINATE_ALL, TERMINATE_NONE, TERMINATE_DEPENDENT
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @Override
    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        if (primaryApplicationContextList != null) {
            for (ApplicationChildContext applicationChildContext : primaryApplicationContextList) {
                buildTreeStructure(applicationChildContext, stringBuilder);
            }
        }
        return stringBuilder.toString();
    }

    private void buildTreeStructure(ApplicationChildContext applicationChildContext, StringBuilder stringBuilder) {
        if (applicationChildContext != null) {
            if (StringUtils.isNotBlank(stringBuilder.toString())) {
                stringBuilder.append(" --> ");
            }
            stringBuilder.append(applicationChildContext.getId());
            for (ApplicationChildContext childContext : applicationChildContext.getApplicationChildContextList()) {
                buildTreeStructure(childContext, stringBuilder);
            }
        }
    }
}
