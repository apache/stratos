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

import org.apache.axiom.om.OMAbstractFactory;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMFactory;
import org.apache.axiom.om.OMNamespace;
import org.apache.axiom.om.util.AXIOMUtil;
import org.apache.axis2.AxisFault;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.task.TaskDescription;
import org.apache.synapse.task.TaskDescriptionFactory;
import org.apache.synapse.task.TaskDescriptionSerializer;
import org.apache.synapse.task.service.TaskManagementService;
import org.wso2.carbon.core.AbstractAdmin;
import org.wso2.carbon.task.TaskManagementException;
import org.wso2.carbon.task.TaskManager;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;
import java.util.*;

public class AutoscalerTaskMgmtAdminService extends AbstractAdmin implements TaskManagementService{

    private static final Log log = LogFactory.getLog(AutoscalerTaskMgmtAdminService.class);

    private TaskSchedulingManager schedulingManager;


    private static final String TASK_EXTENSION_NS =
            "http://www.wso2.org/tasks";

    private static final OMFactory FACTORY = OMAbstractFactory.getOMFactory();

    private static final OMNamespace TASK_OM_NAMESPACE = FACTORY.createOMNamespace(
            TASK_EXTENSION_NS, "task");

    public AutoscalerTaskMgmtAdminService(){}
    
    public AutoscalerTaskMgmtAdminService(ConfigurationContext configurationContext) {
        this.schedulingManager = TaskSchedulingManager.getInstance();
        this.configurationContext = configurationContext;
    }

    public void shutdown() {
        schedulingManager.shutDown(getConfigContext());
    }

    public void addTaskDescription(TaskDescription taskDescription, Map<String, Object> resources)
            throws Exception {
        if (log.isDebugEnabled()) {
            log.debug("Adding the Task " + taskDescription.getName());
        }
        try {
            schedulingManager.scheduleTask(taskDescription, resources, getConfigContext());
        } catch (Exception e) {
            String msg = "Cannot add the task " + taskDescription.getName() + "..";
            log.error(msg, e);  
            throw e;
        }
    }

    /**
     * Passing the Task Description to scheduling manager to actually add it to the Task
     * Description Repository.
     *
     * @param taskDescription TaskDescription
     */
    public void addTaskDescription(TaskDescription taskDescription) {
        if (log.isDebugEnabled()) {
            log.debug("Adding the Task " + taskDescription.getName());
        }
        try {
            Map<String, Object> resources = new HashMap<String, Object>();
            schedulingManager.scheduleTask(taskDescription, resources, getConfigContext());
        } catch (Exception e) {
            String msg = "Cannot add the task";
            log.error(msg, e);
        }
    }

    public void addTaskDescriptionInAnOmElement(String taskElementString) throws
                                                                          TaskManagementException,
                                                                          XMLStreamException {
        OMElement taskElement = AXIOMUtil.stringToOM(taskElementString);
        if (log.isDebugEnabled()) {
            log.debug("Add TaskDescription - Get a Task configuration  :" + taskElement);
        }
        TaskDescription taskDescription = validateAndCreate(taskElement);

        try {
            addTaskDescription(taskDescription);
        } catch (Exception e) {
            try {
                getTaskManager().deleteTaskDescription(taskDescription.getName(),
                        taskDescription.getGroup());
            } catch (Exception ignored) {
            }
            handleException("Error creating a task : " + e.getMessage(), e);
        }
    }



    public void deleteTaskDescription(TaskDescription taskDescription) {
        String taskName = taskDescription.getName();
        if (log.isDebugEnabled()) {
            log.debug("Deleting the task " + taskName);
        }
        try {
            schedulingManager.deleteTaskDescription(taskDescription, getConfigContext());
        } catch (Exception e) {
            log.error("Cannot delete the task " + taskName, e);
        }
    }

    /**
     * Indicating Task Scheduling manager to delete the task with the given task Name.
     *
     * @param taskName taskName
     */
    public void deleteTaskDescription(String taskName) {
        if (log.isDebugEnabled()) {
            log.debug("Deleting the task " + taskName);
        }
        try {
            TaskDescription taskDescription = new TaskDescription();
            taskDescription.setName(taskName);
            schedulingManager.deleteTaskDescription(taskDescription, getConfigContext());
        } catch (Exception e) {
            log.error("Cannot delete the task " + taskName, e);
        }
    }

    /**
     * Notifying the Task Scheduling Manager to delete the previous Task Description from the
     * Task Description Repository and add the edited task.
     *
     * @param taskDescription TaskDescription
     */
    public void editTaskDescription(TaskDescription taskDescription) {
        if (log.isDebugEnabled()) {
            log.debug("Editing the task " + taskDescription.getName());
        }
        if (schedulingManager.isContains(taskDescription.getName(), getConfigContext())) {
            schedulingManager.deleteTaskDescription(taskDescription, getConfigContext());
            schedulingManager.scheduleTask(taskDescription, null, getConfigContext());
        } else {
            log.error("Task " + taskDescription.getName() + "does not exist");
        }
    }

    public void editTaskDescriptionInOmElement(
            String taskElementString) throws TaskManagementException, XMLStreamException {
        OMElement taskElement = AXIOMUtil.stringToOM(taskElementString);
        if (log.isDebugEnabled()) {
            log.debug("Edit TaskDescription - Get a Task configuration  :" + taskElement);
        }
        try {
            editTaskDescription(validateAndCreate(taskElement));
        } catch (Exception e) {
            String msg = "Error editing Task";
            throw new TaskManagementException(msg, e);
        }
    }

    /**
     * Returns the list of Task Descriptions that have been already added to the Task Description
     * Repository.
     *
     * @return A list of Task Descriptions
     */
    public List<TaskDescription> getAllTaskDescriptions() {
        List<TaskDescription> taskDescriptions = new ArrayList<TaskDescription>();
        Iterator<TaskDescription> iterator = schedulingManager.getAllTaskDescriptions(
                getConfigContext());

        while (iterator.hasNext()) {
            TaskDescription taskDescription = iterator.next();
            if (taskDescription != null) {
                taskDescriptions.add(taskDescription);
            }
        }
        if (log.isDebugEnabled()) {
            log.debug("All available Task based Scheduled Functions " + taskDescriptions);
        }
        return taskDescriptions;
    }

    public String getAllTaskDescriptionsInAnOMElement() throws AxisFault {
        OMElement rootElement =
                OMAbstractFactory.getOMFactory().createOMElement(new QName(TASK_EXTENSION_NS,
                        "taskExtension", "task"));
        try {
            List<TaskDescription> descriptions = getAllTaskDescriptions();
            for (TaskDescription taskDescription : descriptions) {
                if (taskDescription != null) {
                    OMElement taskElement =
                            TaskDescriptionSerializer.serializeTaskDescription(TASK_OM_NAMESPACE,
                                                                               taskDescription);
                    validateTaskElement(taskElement);
                    rootElement.addChild(taskElement);
                }
            }
        } catch (TaskManagementException e) {
            String msg = "Error loading all tasks";
            log.error(msg, e);
            throw new AxisFault(msg, e);
        }
        if (log.isDebugEnabled()) {
            log.debug("Returning all TaskDescriptions as  :" + rootElement);
        }
        return rootElement.toString();
    }

    /**
     * Returns the names of job groups that are being executed.
     *
     * @return An array of strings
     */
    public String[] getAllJobGroups() {
        List<String> strings = getTaskManager().getAllJobGroups();
        return strings.toArray(new String[strings.size()]);
    }

    /**
     * Returns the TaskDescription associates with a particular task name
     *
     * @param taskName Task Name
     * @return TaskDescription
     */
    public TaskDescription getTaskDescription(String taskName) {
        return schedulingManager.getTaskDescription(taskName, getConfigContext());
    }

    public boolean isContains(String taskName) {
        return schedulingManager.isContains(taskName, getConfigContext());
    }

    /**
     * @param taskClass taskClass
     * @return list of strings containing properties of the task class
     */
    public List<String> getPropertyNames(String taskClass) {
        return null;
    }

    /**
     * Returns the TaskManager instance currently being used
     *
     * @return TaskManager
     */
    private synchronized TaskManager getTaskManager() {
        return (TaskManager) getConfigContext().getProperty(
                TaskManager.CARBON_TASK_MANAGER);
    }

    /**
     * Validates the OMElement which has a particular TaskDescription
     * serialized into it.
     *
     * @param taskElement taskElement
     * @throws TaskManagementException TaskManagementException
     */
    private static void validateTaskElement(
            OMElement taskElement) throws TaskManagementException {
        if (taskElement == null) {
            handleException("Task Description OMElement can not be found.");
        }
    }

    /**
     * Handles the exception thrown and logs it.
     *
     * @param msg message to be logged
     * @throws TaskManagementException TaskManagementException
     */
    private static void handleException(String msg) throws TaskManagementException {
        log.error(msg);
        throw new TaskManagementException(msg);
    }

    /**
     * Handles the exception thrown and logs it.
     *
     * @param msg message to be logged
     * @param e   exception thrown
     * @throws TaskManagementException TaskManagementException
     */
    private static void handleException(String msg, Exception e) throws TaskManagementException {
        log.error(msg, e);
        throw new TaskManagementException(msg, e);
    }

    /**
     * Validates an OMElement which has a TaskDescription serialized into it
     * and returns the corresponding TaskDescription
     *
     * @param taskElement OMElement containing the TaskDescription
     * @return TaskDescription
     * @throws TaskManagementException TaskManagementException
     */
    private static TaskDescription validateAndCreate(
            OMElement taskElement) throws TaskManagementException {

        validateTaskElement(taskElement);
        TaskDescription taskDescription =
                TaskDescriptionFactory.createTaskDescription(taskElement, TASK_OM_NAMESPACE);
        validateTaskDescription(taskDescription);
        if (log.isDebugEnabled()) {
            log.debug("Task Description : " + taskDescription);
        }
        return taskDescription;
    }

    /**
     * Validates TaskDescriptions
     *
     * @param description TaskDescription
     * @throws TaskManagementException TaskManagementException
     */
    private static void validateTaskDescription(
            TaskDescription description) throws TaskManagementException {
        if (description == null) {
            handleException("Task Description can not be found.");
        }
    }

}
