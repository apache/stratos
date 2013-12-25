package org.apache.stratos.cloud.controller.internal;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.cloud.controller.exception.CloudControllerException;
import org.apache.stratos.cloud.controller.publisher.CartridgeInstanceDataPublisherTask;
import org.apache.stratos.cloud.controller.runtime.FasterLookUpDataHolder;
import org.apache.stratos.cloud.controller.topic.instance.status.InstanceStatusEventMessageDelegator;
import org.apache.stratos.cloud.controller.topology.TopologySynchronizerTask;
import org.apache.stratos.cloud.controller.util.CloudControllerConstants;
import org.apache.stratos.cloud.controller.util.ServiceReferenceHolder;
import org.wso2.carbon.ntask.common.TaskException;
import org.wso2.carbon.ntask.core.TaskInfo;
import org.wso2.carbon.ntask.core.TaskManager;
import org.wso2.carbon.ntask.core.service.TaskService;

import java.util.HashMap;

/**
 * Cloud Controller task scheduler
 */
public class TaskScheduler {
    private static final Log log = LogFactory.getLog(TaskScheduler.class);

    public static void schedule() {
        FasterLookUpDataHolder dataHolder = FasterLookUpDataHolder.getInstance();
        TaskService taskService = ServiceReferenceHolder.getInstance().getTaskService();

        if (dataHolder.getEnableBAMDataPublisher()) {
            // register and schedule, BAM data publisher task
            registerAndScheduleDataPublisherTask(taskService);
        }

        if (dataHolder.getEnableTopologySync()) {
            // start the topology builder thread
            startTopologyBuilder();

            // register and schedule, topology synchronizer task
            registerAndScheduleTopologySyncTask(taskService);
        }
    }

    private static void registerAndScheduleTopologySyncTask(TaskService taskService) {
        TaskInfo taskInfo;
        TaskManager tm = null;

        try {
            if(log.isDebugEnabled()) {
                log.debug("Scheduling topology synchronization task");
            }

            if (!taskService.getRegisteredTaskTypes().contains(CloudControllerConstants.TOPOLOGY_SYNC_TASK_TYPE)) {
                taskService.registerTaskType(CloudControllerConstants.TOPOLOGY_SYNC_TASK_TYPE);
                tm = taskService.getTaskManager(CloudControllerConstants.TOPOLOGY_SYNC_TASK_TYPE);
                FasterLookUpDataHolder dataHolder = FasterLookUpDataHolder.getInstance();
                String cron = dataHolder.getTopologyConfig().getProperty(CloudControllerConstants.CRON_ELEMENT);
                cron = ( cron == null ? CloudControllerConstants.PUB_CRON_EXPRESSION : cron );
                if(log.isDebugEnabled()) {
                    log.debug(String.format("Topology synchronization task cron: %s", cron));
                }
                TaskInfo.TriggerInfo triggerInfo = new TaskInfo.TriggerInfo(cron);
                taskInfo = new TaskInfo(
                        CloudControllerConstants.TOPOLOGY_SYNC_TASK_NAME,
                        TopologySynchronizerTask.class.getName(),
                        new HashMap<String, String>(), triggerInfo);
                tm.registerTask(taskInfo);
                if(log.isDebugEnabled()) {
                    log.debug("Topology synchronization task registered");
                }
            }
            else {
                if(log.isWarnEnabled()) {
                    log.warn("Topology synchronization task already exists");
                }
            }

        } catch (Exception e) {
            String msg = "Error scheduling task: " + CloudControllerConstants.TOPOLOGY_SYNC_TASK_NAME;
            log.error(msg);
            if (tm != null) {
                try {
                    tm.deleteTask(CloudControllerConstants.TOPOLOGY_SYNC_TASK_NAME);
                } catch (TaskException e1) {
                    log.error(e1);
                }
            }
            throw new CloudControllerException(msg, e);
        }
    }

    private static void startTopologyBuilder() {
        // initialize TopologyEventMessageProcessor Consumer
        Thread delegatorThread = new Thread(new InstanceStatusEventMessageDelegator());
        // start consumer
        delegatorThread.start();
    }

    private static TaskManager registerAndScheduleDataPublisherTask(
            TaskService taskService) {
        TaskInfo taskInfo;
        TaskManager tm = null;
        // initialize and schedule the data publisher task
        try {

            if (!taskService.getRegisteredTaskTypes().contains(
                    CloudControllerConstants.DATA_PUB_TASK_TYPE)) {

                taskService
                        .registerTaskType(CloudControllerConstants.DATA_PUB_TASK_TYPE);

                tm = taskService
                        .getTaskManager(CloudControllerConstants.DATA_PUB_TASK_TYPE);

                if (!tm.isTaskScheduled(CloudControllerConstants.DATA_PUB_TASK_NAME)) {
                    FasterLookUpDataHolder dataHolder = FasterLookUpDataHolder.getInstance();
                    TaskInfo.TriggerInfo triggerInfo = new TaskInfo.TriggerInfo(
                            dataHolder.getDataPubConfig().getDataPublisherCron());
                    taskInfo = new TaskInfo(
                            CloudControllerConstants.DATA_PUB_TASK_NAME,
                            CartridgeInstanceDataPublisherTask.class.getName(),
                            new HashMap<String, String>(), triggerInfo);
                    tm.registerTask(taskInfo);

                    // Following code is currently not required, due to an issue
                    // in TS API.
                    // tm.scheduleTask(taskInfo.getName());
                }
            }

        } catch (Exception e) {
            String msg = "Error scheduling task: "
                    + CloudControllerConstants.DATA_PUB_TASK_NAME;
            log.error(msg, e);
            if (tm != null) {
                try {
                    tm.deleteTask(CloudControllerConstants.DATA_PUB_TASK_NAME);
                } catch (TaskException e1) {
                    log.error(e1);
                }
            }
            throw new CloudControllerException(msg, e);
        }
        return tm;
    }
}
