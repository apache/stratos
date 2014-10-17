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

package org.apache.stratos.messaging.domain.topology.lifecycle;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.messaging.event.topology.TopologyEvent;

import java.io.Serializable;
import java.util.Stack;

public class LifeCycleStateManager<T extends LifeCycleState> implements Serializable {

    private static Log log = LogFactory.getLog(LifeCycleStateManager.class);

    private Stack<T> stateStack;

    public LifeCycleStateManager(T initialState) {
        stateStack = new Stack<T>();
        stateStack.push(initialState);
        //if (log.isDebugEnabled()) {
            log.info("Life Cycle State Manager created, initial state: " + initialState.toString());
        //}
    }

    public <S extends TopologyEvent> boolean isPreConditionsValid (T nextState, S topologyEvent) {
        // TODO: implement
        return true;
    }

    public boolean isStateTransitionValid (T nextState) {

        return stateStack.peek().getNextStates().contains(nextState);
    }

    public void changeState (T nextState)  {

        stateStack.push(nextState);
    }

    public Stack<T> getStateStack () {
        return stateStack;
    }

    public T getCurrentState () {
        return stateStack.peek();
    }

    public T getPreviousState () {
        return stateStack.get(1);
    }
}
