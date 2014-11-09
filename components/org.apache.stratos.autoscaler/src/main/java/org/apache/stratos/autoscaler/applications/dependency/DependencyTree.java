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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.autoscaler.applications.dependency.context.ApplicationContext;

import java.util.ArrayList;
import java.util.List;

/**
 * This is to contain the dependency tree of an application/group
 */
public class DependencyTree {
    private static final Log log = LogFactory.getLog(DependencyTree.class);

    private List<ApplicationContext> applicationContextList;

    private boolean started;

    private boolean terminated;

    private TerminationBehavior terminationBehavior;

    private boolean startupOder;

    private boolean reverseStartupOrder;

    private String id;

    public DependencyTree(String id) {
        applicationContextList = new ArrayList<ApplicationContext>();
        this.setId(id);
        if (log.isDebugEnabled()) {
            log.debug("Starting a dependency tree for the [group/application] " + id);
        }
    }

    public List<ApplicationContext> getApplicationContextList() {
        return applicationContextList;
    }

    public void setApplicationContextList(List<ApplicationContext> applicationContextList) {
        this.applicationContextList = applicationContextList;
    }

    public void addApplicationContext(ApplicationContext applicationContext) {
        applicationContextList.add(applicationContext);

    }

    /**
     * Find an ApplicationContext from dependency tree with the given id
     *
     * @param id the alias/id of group/cluster
     * @return ApplicationContext of the given id
     */
    public ApplicationContext findApplicationContextWithId(String id) {
        return findApplicationContextWithId(id, applicationContextList);
    }

    /**
     * Find the ApplicationContext using Breadth first search.
     *
     * @param id       the alias/id of group/cluster
     * @param contexts the list of contexts in the same level of the tree
     * @return ApplicationContext of the given id
     */
    private ApplicationContext findApplicationContextWithId(String id, List<ApplicationContext> contexts) {
        for (ApplicationContext context : contexts) {
            //TODO check for the status
            if (context.getId().equals(id)) {
                return context;
            }
        }
        //if not found in the top level search recursively
        for (ApplicationContext context : contexts) {
            return findApplicationContextWithId(id, context.getApplicationContextList());
        }
        return null;
    }

    public ApplicationContext findParentContextWithId(String id) {
        return findParentContextWithId(null, id, this.applicationContextList);
    }

    public List<ApplicationContext> findAllParentContextWithId(String id) {
        List<ApplicationContext> applicationContexts = new ArrayList<ApplicationContext>();
        return findAllParent(applicationContexts, id);
    }

    private List<ApplicationContext> findAllParent(List<ApplicationContext> parentContexts, String id) {
        ApplicationContext context = findParentContextWithId(null, id, this.applicationContextList);
        if (context != null) {
            parentContexts.add(context);
            findAllParent(parentContexts, context.getId());
        }
        return parentContexts;
    }


    private ApplicationContext findParentContextWithId(ApplicationContext parent, String id,
                                                       List<ApplicationContext> contexts) {
        for (ApplicationContext context : contexts) {
            //TODO check for the status
            if (context.getId().equals(id)) {
                return parent;
            }
        }
        //if not found in the top level search recursively
        for (ApplicationContext context : this.applicationContextList) {
            return findParentContextWithId(context, id, context.getApplicationContextList());
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
    public List<ApplicationContext> getStarAbleDependencies(String id) {
        //finding the application context which received the activated event and
        // returning it's immediate children as the dependencies.
        ApplicationContext context = findApplicationContextWithId(id);
        return context.getApplicationContextList();
    }

    /**
     * Getting the next start able dependencies upon the monitor initialization.
     *
     * @return list of dependencies
     */
    public List<ApplicationContext> getStarAbleDependencies() {
        //returning the top level as the monitor is in initializing state
        return this.applicationContextList;
    }

    public List<ApplicationContext> getStarAbleDependenciesByTermination() {
        //Breadth First search over the graph to find out which level has the terminated contexts
        return traverseGraphByLevel(this.applicationContextList);
    }


    private List<ApplicationContext> traverseGraphByLevel(List<ApplicationContext> contexts) {
        for(ApplicationContext context : contexts) {
            if(context.isTerminated()) {
                return contexts;
            }
        }

        for(ApplicationContext context : contexts) {
            return traverseGraphByLevel(context.getApplicationContextList());
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
    public List<ApplicationContext> getTerminationDependencies(String id) {
        List<ApplicationContext> allChildrenOfAppContext = new ArrayList<ApplicationContext>();
        ApplicationContext applicationContext = findApplicationContextWithId(id);

        if (terminationBehavior == TerminationBehavior.TERMINATE_DEPENDENT) {
            //finding the ApplicationContext of the given id
            //finding all the children of the found application context
            allChildrenOfAppContext.add(applicationContext);
            findAllChildrenOfAppContext(applicationContext.getApplicationContextList(),
                    allChildrenOfAppContext);
            return allChildrenOfAppContext;
        } else if (terminationBehavior == TerminationBehavior.TERMINATE_DEPENDENT) {
            //killall will be killed by the monitor from it's list.
            findAllChildrenOfAppContext(this.applicationContextList,
                    allChildrenOfAppContext);

        }
        //return empty for the kill-none case, what ever returns here will be killed in
        return allChildrenOfAppContext;
    }

    /**
     * This will help to find out all the children of a particular node
     *
     * @param applicationContexts app contexts of the particular node
     * @param childContexts       contains the children of the node
     * @return all the children of the given node
     */
    public List<ApplicationContext> findAllChildrenOfAppContext(List<ApplicationContext> applicationContexts,
                                                                List<ApplicationContext> childContexts) {
        for (ApplicationContext context : applicationContexts) {
            childContexts.add(context);
            findAllChildrenOfAppContext(context.getApplicationContextList(), childContexts);
        }
        return childContexts;
    }

    public void setTerminationBehavior(TerminationBehavior terminationBehavior) {
        this.terminationBehavior = terminationBehavior;
    }

    public boolean isTerminateDependent() {
        return this.terminationBehavior == TerminationBehavior.TERMINATE_DEPENDENT;
    }

    public boolean isTerminateAll() {
        return this.terminationBehavior == TerminationBehavior.TERMINATE_ALL;
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

    public boolean isStartupOder() {
        return startupOder;
    }

    public void setStartupOder(boolean startupOder) {
        this.startupOder = startupOder;
    }

    public boolean isReverseStartupOrder() {
        return reverseStartupOrder;
    }

    public void setReverseStartupOrder(boolean reverseStartupOrder) {
        this.reverseStartupOrder = reverseStartupOrder;
    }

}
