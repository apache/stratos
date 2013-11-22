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

import org.apache.log4j.Logger;
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

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@SiddhiExtension(namespace = "stratos", function = "faultHandling")
public class FaultHandlingWindowProcessor extends WindowProcessor implements RunnableWindowProcessor {

    private static final int MILI_TO_MINUTE = 1000;
    private static final int TIME_OUT = 100;

    static final Logger log = Logger.getLogger(FaultHandlingWindowProcessor.class);
    private ScheduledExecutorService eventRemoverScheduler;
    private int subjectedAttrIndex;
    private ThreadBarrier threadBarrier;
    private long timeToKeep;
    private ISchedulerSiddhiQueue<StreamEvent> window;
    private ConcurrentHashMap<String, InEvent> timeStampMap = new ConcurrentHashMap<String, InEvent>();
    private String memberID;

    @Override
    protected void processEvent(InEvent event) {
        addDataToMap(event);
    }

    @Override
    protected void processEvent(InListEvent listEvent) {
        System.out.println(listEvent);
        for (int i = 0, size = listEvent.getActiveEvents(); i < size; i++) {
            addDataToMap((InEvent) listEvent.getEvent(i));
        }
    }

    protected void addDataToMap(InEvent event) {
        if (memberID != null) {
            String id = (String)event.getData()[subjectedAttrIndex];
            timeStampMap.put(id, event);
        }
        else {
            System.out.println("Member ID null");
            log.error("NULL Member ID");
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

    @Override
    public void run() {
        try {
            while (true) {
                threadBarrier.pass();
                Iterator it = timeStampMap.entrySet().iterator();

                while ( it.hasNext() ) {
                    Map.Entry pair = (Map.Entry)it.next();
                    long currentTime = System.currentTimeMillis();
                    InEvent event = (InEvent)pair.getValue();

                    if ((currentTime - event.getTimeStamp()) / MILI_TO_MINUTE > TIME_OUT) {
                        log.info("Member Inactive : " + pair.getKey());
                        it.remove();
                        nextProcessor.process(event);
                    }
                }
            }
        } catch (Throwable t) {
            log.error(t.getMessage(), t);
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
    protected void init(Expression[] parameters, QueryPostProcessingElement nextProcessor, AbstractDefinition streamDefinition, String elementId, boolean async, SiddhiContext siddhiContext) {
        if (parameters[0] instanceof IntConstant) {
            timeToKeep = ((IntConstant) parameters[0]).getValue();
        } else {
            timeToKeep = ((LongConstant) parameters[0]).getValue();
        }

        memberID = ((Variable)parameters[1]).getAttributeName();

        String subjectedAttr = ((Variable)parameters[1]).getAttributeName();
        subjectedAttrIndex = streamDefinition.getAttributePosition(subjectedAttr);

        if (this.siddhiContext.isDistributedProcessingEnabled()) {
            window = new SchedulerSiddhiQueueGrid<StreamEvent>(elementId, this, this.siddhiContext, this.async);
        } else {
            window = new SchedulerSiddhiQueue<StreamEvent>(this);
        }

        //Ordinary scheduling
        window.schedule();

    }

    @Override
    public void schedule() {
        eventRemoverScheduler.schedule(this, timeToKeep, TimeUnit.MILLISECONDS);
    }

    public void scheduleNow() {
        eventRemoverScheduler.schedule(this, 0, TimeUnit.MILLISECONDS);
    }

    @Override
    public void setScheduledExecutorService(ScheduledExecutorService scheduledExecutorService) {
        this.eventRemoverScheduler = scheduledExecutorService;
    }

    public void setThreadBarrier(ThreadBarrier threadBarrier) {
        this.threadBarrier = threadBarrier;
    }

    @Override
    public void destroy(){
        window = null;
    }
}
