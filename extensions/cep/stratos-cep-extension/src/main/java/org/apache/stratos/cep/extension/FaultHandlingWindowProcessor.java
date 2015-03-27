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
package org.apache.stratos.cep.extension;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.apache.stratos.common.threading.StratosThreadPool;
import org.apache.stratos.messaging.broker.publish.EventPublisher;
import org.apache.stratos.messaging.broker.publish.EventPublisherPool;
import org.apache.stratos.messaging.domain.topology.*;
import org.apache.stratos.messaging.event.health.stat.MemberFaultEvent;
import org.apache.stratos.messaging.message.receiver.topology.TopologyManager;
import org.apache.stratos.messaging.util.MessagingUtil;
import org.wso2.siddhi.core.config.SiddhiContext;
import org.wso2.siddhi.core.event.StreamEvent;
import org.wso2.siddhi.core.event.in.InEvent;
import org.wso2.siddhi.core.event.in.InListEvent;
import org.wso2.siddhi.core.persistence.ThreadBarrier;
import org.wso2.siddhi.core.query.QueryPostProcessingElement;
import org.wso2.siddhi.core.query.processor.window.RunnableWindowProcessor;
import org.wso2.siddhi.core.query.processor.window.WindowProcessor;
import org.wso2.siddhi.core.util.collection.queue.scheduler.ISchedulerSiddhiQueue;
import org.wso2.siddhi.core.util.collection.queue.scheduler.SchedulerSiddhiQueue;
import org.wso2.siddhi.core.util.collection.queue.scheduler.SchedulerSiddhiQueueGrid;
import org.wso2.siddhi.query.api.definition.AbstractDefinition;
import org.wso2.siddhi.query.api.expression.Expression;
import org.wso2.siddhi.query.api.expression.Variable;
import org.wso2.siddhi.query.api.expression.constant.IntConstant;
import org.wso2.siddhi.query.api.expression.constant.LongConstant;
import org.wso2.siddhi.query.api.extension.annotation.SiddhiExtension;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * CEP window processor to handle faulty member instances. This window processor is responsible for
 * publishing MemberFault event if health stats are not received within a given time window.
 */
@SiddhiExtension(namespace = "stratos", function = "faultHandling")
public class FaultHandlingWindowProcessor extends WindowProcessor implements RunnableWindowProcessor {

	private static final Logger log = Logger.getLogger(FaultHandlingWindowProcessor.class);

    private static final int TIME_OUT = 60 * 1000;
    public static final String CEP_EXTENSION_THREAD_POOL_KEY = "cep.extension.thread.pool";
    public static final int CEP_EXTENSION_THREAD_POOL_SIZE = 10;

    private ExecutorService executorService;
    private ScheduledExecutorService faultHandleScheduler;
	private ThreadBarrier threadBarrier;
	private long timeToKeep;
	private ISchedulerSiddhiQueue<StreamEvent> window;
	private EventPublisher healthStatPublisher =
			EventPublisherPool.getPublisher(MessagingUtil.Topics.HEALTH_STAT_TOPIC.getTopicName());
	private Map<String, Object> MemberFaultEventMap = new HashMap<String, Object>();
	private Map<String, Object> memberFaultEventMessageMap = new HashMap<String, Object>();

	// Map of member id's to their last received health event time stamp
	private ConcurrentHashMap<String, Long> memberTimeStampMap = new ConcurrentHashMap<String, Long>();

	// Event receiver to receive topology events published by cloud-controller
	private CEPTopologyEventReceiver cepTopologyEventReceiver = new CEPTopologyEventReceiver(this);

	// Stratos member id attribute index in stream execution plan
	private int memberIdAttrIndex;

	@Override
	protected void processEvent(InEvent event) {
		addDataToMap(event);
	}

	@Override
	protected void processEvent(InListEvent listEvent) {
		for (int i = 0, size = listEvent.getActiveEvents(); i < size; i++) {
			addDataToMap((InEvent) listEvent.getEvent(i));
		}
	}

	/**
	 * Add new entry to time stamp map from the received event.
	 *
	 * @param event Event received by Siddhi.
	 */
	protected void addDataToMap(InEvent event) {
		String id = (String) event.getData()[memberIdAttrIndex];
		//checking whether this member is the topology.
		//sometimes there can be a delay between publishing member terminated events
		//and actually terminating instances. Hence CEP might get events for already terminated members
		//so we are checking the topology for the member existence
		Member member = getMemberFromId(id);
		if (null == member) {
			log.debug("Member not found in the topology. Event rejected");
			return;
		}
        if (StringUtils.isNotEmpty(id)) {
            memberTimeStampMap.put(id, event.getTimeStamp());
        } else {
            log.warn("NULL member id found in the event received. Event rejected.");
        }
        if (log.isDebugEnabled()){
            log.debug("Event received from [member-id] " + id + " [time-stamp] " + event.getTimeStamp());
        }
    }

    @Override
    public Iterator<StreamEvent> iterator() {
        return window.iterator();
    }

    @Override
    public Iterator<StreamEvent> iterator(String predicate) {
        if (siddhiContext.isDistributedProcessingEnabled()) {
            return ((SchedulerSiddhiQueueGrid<StreamEvent>) window).iterator(predicate);
        } else {
            return window.iterator();
        }
    }

    /**
     *  Retrieve the current activated members from the topology and initialize the timestamp map.
     *  This will allow the system to recover from a restart
     *
     *  @param topology Topology model object
     */
    boolean loadTimeStampMapFromTopology(Topology topology){

        long currentTimeStamp = System.currentTimeMillis();
        if (topology == null || topology.getServices() == null){
            return false;
        }
        // TODO make this efficient by adding APIs to messaging component
        for (Service service : topology.getServices()) {
            if (service.getClusters() != null) {
                for (Cluster cluster : service.getClusters()) {
                    if (cluster.getMembers() != null) {
                        for (Member member : cluster.getMembers()) {
                            // we are checking faulty status only in previously activated members
                            if (member != null && MemberStatus.Active.equals(member.getStatus())) {
                                // Initialize the member time stamp map from the topology at the beginning
                                memberTimeStampMap.putIfAbsent(member.getMemberId(), currentTimeStamp);
                            }
                        }
                    }
                }
            }
        }

        if (log.isDebugEnabled()){
            log.debug("Member timestamps were successfully loaded from the topology: [timestamps] " +
                    memberTimeStampMap);
        }
        return true;
    }

    private Member getMemberFromId(String memberId){
        if (StringUtils.isEmpty(memberId)){
            return null;
        }
        if (TopologyManager.getTopology().isInitialized()){
        	try {
                TopologyManager.acquireReadLock();
                if (TopologyManager.getTopology().getServices() == null){
                    return null;
                }
                // TODO make this efficient by adding APIs to messaging component
                for (Service service : TopologyManager.getTopology().getServices()) {
                    if (service.getClusters() != null) {
                        for (Cluster cluster : service.getClusters()) {
                            if (cluster.getMembers() != null) {
                                for (Member member : cluster.getMembers()){
                                    if (memberId.equals(member.getMemberId())){
                                        return member;
                                    }
                                }
                            }
                        }
                    }
                }
        	} catch (Exception e) {
        		log.error("Error while reading topology" + e);
        	} finally {
        		TopologyManager.releaseReadLock();
        	}
        }
        return null;
    }

    private void publishMemberFault(String memberId){
        Member member = getMemberFromId(memberId);
        if (member == null){
            log.error("Failed to publish member fault event. Member having [member-id] " + memberId +
                    " does not exist in topology");
            return;
        }
        log.info("Publishing member fault event for [member-id] " + memberId);

        MemberFaultEvent memberFaultEvent = new MemberFaultEvent(member.getClusterId(), member.getClusterInstanceId(),
                member.getMemberId(), member.getPartitionId(),
                member.getNetworkPartitionId(), 0);

        memberFaultEventMessageMap.put("message", memberFaultEvent);
        healthStatPublisher.publish(MemberFaultEventMap, true);
    }

    @Override
    public void run() {
        try {
            threadBarrier.pass();

            for (Object o : memberTimeStampMap.entrySet()) {
                Map.Entry pair = (Map.Entry) o;
                long currentTime = System.currentTimeMillis();
                Long eventTimeStamp = (Long) pair.getValue();

                if ((currentTime - eventTimeStamp) > TIME_OUT) {
                    log.info("Faulty member detected [member-id] " + pair.getKey() + " with [last time-stamp] " +
                            eventTimeStamp + " [time-out] " + TIME_OUT + " milliseconds");
                    publishMemberFault((String) pair.getKey());
                }
            }
            if (log.isDebugEnabled()){
                log.debug("Fault handling processor iteration completed with [time-stamp map length] " +
                        memberTimeStampMap.size() + " [time-stamp map] " + memberTimeStampMap);
            }
        } catch (Throwable t) {
            log.error(t.getMessage(), t);
        } finally {
            faultHandleScheduler.schedule(this, timeToKeep, TimeUnit.MILLISECONDS);
        }
    }

    @Override
    protected Object[] currentState() {
        return new Object[]{window.currentState()};
    }

    @Override
    protected void restoreState(Object[] data) {
        window.restoreState(data);
        window.restoreState((Object[]) data[0]);
        window.reSchedule();
    }

    @Override
    protected void init(Expression[] parameters, QueryPostProcessingElement nextProcessor,
                        AbstractDefinition streamDefinition, String elementId, boolean async, SiddhiContext siddhiContext) {

        if (parameters[0] instanceof IntConstant) {
            timeToKeep = ((IntConstant) parameters[0]).getValue();
        } else {
            timeToKeep = ((LongConstant) parameters[0]).getValue();
        }

        String memberIdAttrName = ((Variable) parameters[1]).getAttributeName();
        memberIdAttrIndex = streamDefinition.getAttributePosition(memberIdAttrName);

        if (this.siddhiContext.isDistributedProcessingEnabled()) {
            window = new SchedulerSiddhiQueueGrid<StreamEvent>(elementId, this, this.siddhiContext, this.async);
        } else {
            window = new SchedulerSiddhiQueue<StreamEvent>(this);
        }
        MemberFaultEventMap.put("org.apache.stratos.messaging.event.health.stat.MemberFaultEvent", memberFaultEventMessageMap);

	    executorService = StratosThreadPool.getExecutorService(CEP_EXTENSION_THREAD_POOL_KEY,
                CEP_EXTENSION_THREAD_POOL_SIZE);
	    cepTopologyEventReceiver.setExecutorService(executorService);
        cepTopologyEventReceiver.execute();

        //Ordinary scheduling
        window.schedule();
        if (log.isDebugEnabled()){
            log.debug("Fault handling window processor initialized with [timeToKeep] " + timeToKeep +
                    ", [memberIdAttrName] " + memberIdAttrName + ", [memberIdAttrIndex] " + memberIdAttrIndex +
                    ", [distributed-enabled] " + this.siddhiContext.isDistributedProcessingEnabled());
        }
    }

    @Override
    public void schedule() {
        faultHandleScheduler.schedule(this, timeToKeep, TimeUnit.MILLISECONDS);
    }

    @Override
    public void scheduleNow() {
        faultHandleScheduler.schedule(this, 0, TimeUnit.MILLISECONDS);
    }

    @Override
    public void setScheduledExecutorService(ScheduledExecutorService scheduledExecutorService) {
        this.faultHandleScheduler = scheduledExecutorService;
    }

    @Override
    public void setThreadBarrier(ThreadBarrier threadBarrier) {
        this.threadBarrier = threadBarrier;
    }

    @Override
    public void destroy(){
        // terminate topology listener thread
        cepTopologyEventReceiver.terminate();
        window = null;

        // Shutdown executor service
        if(executorService != null) {
            try {
                executorService.shutdownNow();
            } catch (Exception e) {
                log.warn("An error occurred while shutting down cep extension executor service", e);
            }
        }
    }

    public ConcurrentHashMap<String, Long> getMemberTimeStampMap() {
        return memberTimeStampMap;
    }
}
