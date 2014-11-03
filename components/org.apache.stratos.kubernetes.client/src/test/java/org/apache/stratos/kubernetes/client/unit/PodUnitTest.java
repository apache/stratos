/*
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 *
 */
package org.apache.stratos.kubernetes.client.unit;

import junit.framework.TestCase;

import org.apache.stratos.kubernetes.client.model.Container;
import org.apache.stratos.kubernetes.client.model.Label;
import org.apache.stratos.kubernetes.client.model.Manifest;
import org.apache.stratos.kubernetes.client.model.Pod;
import org.apache.stratos.kubernetes.client.model.Port;
import org.apache.stratos.kubernetes.client.model.State;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@Category(org.apache.stratos.kubernetes.client.UnitTests.class)
public class PodUnitTest extends TestCase{

	@Before
	public void setUp() {
	}
	
	@Test
	public void testPods() throws Exception { 
	    String podId = "nirmal-test-pod";
	    String time = "2014/11/02";
	    String selfLink = "link";
        Pod pod = new Pod();
        String apiVersion = "v1beta1";
        pod.setApiVersion(apiVersion);
        pod.setId(podId);
        pod.setCreationTimestamp(time);
        pod.setSelfLink(selfLink);
        pod.setResourceVersion(apiVersion);
        String kind = "Pod";
        pod.setKind(kind);
        Label l = new Label();
        l.setName("nirmal");
        pod.setLabels(l);
        State desiredState = new State();
        Manifest m = new Manifest();
        m.setId(podId);
        m.setVersion(apiVersion);
        Container c = new Container();
        c.setName("master");
        c.setImage("image");
        Port p = new Port();
        p.setContainerPort(8379);
        p.setHostPort(8379);
        c.setPorts(new Port[] { p });
        m.setContainers(new Container[] { c });
        desiredState.setManifest(m);
        pod.setDesiredState(desiredState);
        State currentState = desiredState;
        pod.setCurrentState(currentState);
        
        assertEquals(podId, pod.getId());
        assertEquals(apiVersion, pod.getApiVersion());
        assertEquals(apiVersion, pod.getResourceVersion());
        assertEquals(kind, pod.getKind());
        assertEquals(l, pod.getLabels());
        assertEquals(currentState, pod.getCurrentState());
        assertEquals(selfLink, pod.getSelfLink());
        assertEquals(desiredState, pod.getDesiredState());
        assertEquals(time, pod.getCreationTimestamp());
        
        assertEquals(true, pod.equals(pod));
        
        Pod pod2 = new Pod();
        pod2.setId(podId);
        
        assertEquals(true, pod.equals(pod2));
        assertEquals(true, pod.hashCode() == pod2.hashCode());
        
        pod2.setId("aa");
        assertEquals(false, pod.equals(pod2));
        
        pod2.setId(null);
        assertEquals(false, pod.equals(pod2));
        
        assertEquals(false, pod.equals(null));
        assertEquals(false, pod.equals(desiredState));
        
        pod.setId(null);
        pod2.setId(podId);
        assertEquals(false, pod.equals(pod2));
        
        pod2.setId(null);
        assertEquals(true, pod.equals(pod2));
        assertEquals(true, pod.hashCode() == pod2.hashCode());
        
	}
}
