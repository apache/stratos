/**
 * Copyright (c) WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.wso2.carbon.mediator.autoscale.lbautoscale.task;

import org.apache.axis2.context.ConfigurationContext;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.task.TaskDescription;
import org.apache.synapse.task.TaskDescriptionRepository;
import org.apache.synapse.task.TaskScheduler;
import org.wso2.carbon.task.TaskManager;

import java.util.Iterator;
import java.util.Map;

public class TaskSchedulingManager {

    private static final Log log = LogFactory.getLog(TaskSchedulingManager.class);

    private static TaskSchedulingManager thisInstance = new TaskSchedulingManager();

    public static TaskSchedulingManager getInstance() {
        return thisInstance;
    }

    /**
     * This method adds a particular Task to the Task Description Repository and schedule it
     * for the execution.
     *
     * @param taskDescription      TaskDescription
     * @param resources            Map containing require meta data for the task execution.
     * @param configurationContext ConfigurationContext
     */
    public void scheduleTask(TaskDescription taskDescription, Map<String, Object> resources,
                             ConfigurationContext configurationContext) {
        if (log.isDebugEnabled()) {
            log.debug("Adding a Task Description to the Task Description Repository");
        }

        getTaskDescriptionRepository(configurationContext).addTaskDescription(taskDescription);
        getTaskScheduler(configurationContext).scheduleTask(
                taskDescription, resources, AutoscalingJob.class);

        if (log.isDebugEnabled()) {
            log.debug("Task Description " + taskDescription.getName() +
                    " added to the Task Description Repository");
        }
    }

    /**
     * Returns a Task Description with a given name from the Task Description Repository.
     *
     * @param taskName             taskName
     * @param configurationContext ConfigurationContext
     * @return TaskDescription
     */
    public TaskDescription getTaskDescription(
            String taskName, ConfigurationContext configurationContext) {
        if (log.isDebugEnabled()) {
            log.debug("Returning a Start up : " + taskName + " from the configuration");
        }

        TaskDescription taskDescription = getTaskDescriptionRepository(
                configurationContext).getTaskDescription(taskName);

        if (taskDescription != null) {
            if (log.isDebugEnabled()) {
                log.debug("Returning a Task Description : " + taskDescription);

            }
            return taskDescription;
        } else {
            if (log.isDebugEnabled()) {
                log.debug("No Task Description can be found with name :" + taskName);
            }
            return null;
        }
    }

    /**
     * This method search for a given Task which has already been added to the Task Description
     * Repository and removes it.
     *
     * @param taskDescription             TaskDescription
     * @param configurationContext ConfigurationContext
     */
    public void deleteTaskDescription(TaskDescription taskDescription, ConfigurationContext configurationContext) {
        String taskName = null;
        if (taskDescription != null) {
            taskName = taskDescription.getName();
            getTaskDescriptionRepository(configurationContext).removeTaskDescription(taskName);
            getTaskScheduler(configurationContext).deleteTask(taskName, taskDescription.getGroup());

            if (log.isDebugEnabled()) {
                log.debug("Deleted TaskDescription : " + taskName + " from the configuration");
            }
        } else {
            log.warn("Unable to delete the Task " + taskName
                    + ",as it doesn't exist in the Repository");
        }
    }

    /**
     * Retrieves all Task Descriptions added to the Task Description Repository at a given instance.
     *
     * @param configurationContext ConfigurationContext
     * @return TaskDescription Iterator
     */
    public Iterator<TaskDescription> getAllTaskDescriptions(
            ConfigurationContext configurationContext) {
        if (log.isDebugEnabled()) {
            log.debug("Returning a All TaskDescription from the configuration");
        }
        return getTaskDescriptionRepository(configurationContext).getAllTaskDescriptions();
    }

    /**
     * This method checks whether a particular Task has already been added to the Task Description
     * Repository associated with the context of this execution and returns a flag indicating the
     * existence.
     *
     * @param taskName             Name of the task to be searched
     * @param configurationContext ConfigurationContext
     * @return a boolean depending on the existence of a task
     */
    public boolean isContains(String taskName, ConfigurationContext configurationContext) {
        return !getTaskDescriptionRepository(configurationContext).isUnique(taskName);
    }

    /**
     * Returns the Carbon TaskDescriptionRepository instance that carries details of the added
     * tasks for execution.
     *
     * @param configurationContext ConfigurationContext
     * @return TaskDescriptionRepository
     */
    private synchronized TaskDescriptionRepository getTaskDescriptionRepository(
            ConfigurationContext configurationContext) {
        if (log.isDebugEnabled()) {
            log.debug("Retrieving access to Task Description Repository");
        }
        return (TaskDescriptionRepository) configurationContext.getProperty(
                TaskManager.CARBON_TASK_REPOSITORY);
    }

    /**
     * Returns the carbon TaskScheduler associated with the context of scheduling the task execution
     *
     * @param configurationContext ConfigurationContext
     * @return TaskScheduler
     */
    private synchronized TaskScheduler getTaskScheduler(ConfigurationContext configurationContext) {
        if (log.isDebugEnabled()) {
            log.debug("Retrieving access to Task Scheduler");
        }
        return (TaskScheduler) configurationContext.getProperty(
                TaskManager.CARBON_TASK_SCHEDULER);
    }

    public void shutDown(ConfigurationContext configurationContext) {
        if (log.isDebugEnabled()) {
            log.debug("Starting to shut down tasks");
        }

//        getTaskDescriptionRepository(configurationContext).addTaskDescription(taskDescription);
        getTaskScheduler(configurationContext).shutDown();

        if (log.isDebugEnabled()) {
            log.debug("All tasks shut down");
        }
    }
}
