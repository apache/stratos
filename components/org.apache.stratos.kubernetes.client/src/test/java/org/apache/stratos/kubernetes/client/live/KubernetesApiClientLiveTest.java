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
package org.apache.stratos.kubernetes.client.live;

import junit.framework.TestCase;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.kubernetes.client.KubernetesApiClient;
import org.apache.stratos.kubernetes.client.exceptions.KubernetesClientException;
import org.apache.stratos.kubernetes.client.model.Container;
import org.apache.stratos.kubernetes.client.model.Label;
import org.apache.stratos.kubernetes.client.model.Manifest;
import org.apache.stratos.kubernetes.client.model.Pod;
import org.apache.stratos.kubernetes.client.model.Port;
import org.apache.stratos.kubernetes.client.model.ReplicationController;
import org.apache.stratos.kubernetes.client.model.Selector;
import org.apache.stratos.kubernetes.client.model.Service;
import org.apache.stratos.kubernetes.client.model.State;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@Category(org.apache.stratos.kubernetes.client.LiveTests.class)
public class KubernetesApiClientLiveTest extends TestCase{

    private static final Log log = LogFactory.getLog(KubernetesApiClientLiveTest.class);
    private KubernetesApiClient client;
    private String dockerImage;
    
	@Before
	public void setUp() {
	    String endpoint = System.getProperty("kubernetes.api.endpoint");
	    if (endpoint == null) {
	        endpoint = "http://192.168.1.100:8080/api/v1beta1/";
	    }
        log.info("Provided Kubernetes endpoint using system property [kubernetes.api.endpoint] : " +endpoint);
	    client = new KubernetesApiClient(endpoint);
	    
	    // image should be pre-downloaded for ease of testing.
	    dockerImage = System.getProperty("docker.image");
	    if (dockerImage == null) {
	        dockerImage = "gurpartap/redis";
	    }
	}
	
	@Test
	public void testPods() throws Exception { 
	    log.info("Testing Pods ....");
	    String podId = "nirmal-test-pod";
        Pod pod = new Pod();
        pod.setApiVersion("v1beta1");
        pod.setId(podId);
        pod.setKind("Pod");
        Label l = new Label();
        l.setName("nirmal");
        pod.setLabels(l);
        State desiredState = new State();
        Manifest m = new Manifest();
        m.setId(podId);
        m.setVersion("v1beta1");
        Container c = new Container();
        c.setName("master");
        c.setImage(dockerImage);
        Port p = new Port();
        p.setContainerPort(8379);
        p.setHostPort(8379);
        c.setPorts(new Port[] { p });
        m.setContainers(new Container[] { c });
        desiredState.setManifest(m);
        pod.setDesiredState(desiredState);
        if (log.isDebugEnabled()) {
            log.debug("Creating a Pod "+pod);
        }
        client.createPod(pod);
	    assertNotNull(client.getPod(podId));
	    
	    // give 2s to download the image
	    Thread.sleep(2000);
	    
	    // test recreation from same id
	    client.createPod(pod);
	    assertNotNull(client.getPod(podId));
	    
	    String bogusPodId = "nirmal";
	    // create an invalid Pod
	    Pod pod3 = new Pod();
	    pod3.setId(bogusPodId);
	    try {
	        client.createPod(pod3);
	    } catch (Exception e) {
	        assertEquals(true, e instanceof KubernetesClientException);
	    }
	    
	    try {
	        client.getPod(bogusPodId);
	    } catch (Exception e) {
	        assertEquals(true, e instanceof KubernetesClientException);
	        assertEquals("Pod ["+bogusPodId+"] doesn't exist.", e.getMessage());
	    }
	    
	    if (log.isDebugEnabled()) {
            log.debug("Get all Pods ");
        }
	    Pod[] currentPods = client.getAllPods();
	    boolean match = false;
	    for (Pod pod2 : currentPods) {
            if (podId.equals(pod2.getId())) {
                match = true;
                break;
            }
        }
	    assertEquals(true, match);
	    
	    Pod[] selectedPods = client.getSelectedPods(new Label[]{l});
	    assertEquals(1, selectedPods.length);
	    
	    if (log.isDebugEnabled()) {
	        log.debug("Deleting a Pod "+pod);
	    }
	    client.deletePod(podId);
	    try {
	        client.getPod(podId);
	    } catch(Exception e) {
	        assertEquals(true, e instanceof KubernetesClientException);
	    }
	    
	    // delete a non-existing pod
	    try {
	        client.deletePod(bogusPodId);
	    } catch (Exception e) {
	        assertEquals(true, e instanceof KubernetesClientException);
	    }
	    
	    selectedPods = client.getSelectedPods(new Label[]{l});
        assertEquals(0, selectedPods.length);
        
        Label ll = new Label();
        ll.setName("nirmal2");
        selectedPods = client.getSelectedPods(new Label[]{l, ll});
        assertEquals(0, selectedPods.length);
        
        selectedPods = client.getSelectedPods(new Label[]{});
        assertEquals(0, selectedPods.length);
	}
	
	@Test
    public void testReplicationControllers() throws Exception { 
	    String id = "nirmalController";
	    int replicas = 2;
	    
        ReplicationController contr = new ReplicationController();
        contr.setId(id);
        contr.setKind("ReplicationController");
        contr.setApiVersion("v1beta1");
        State desiredState = new State();
        desiredState.setReplicas(replicas);
        Selector selector = new Selector();
        selector.setName("nirmal");
        desiredState.setReplicaSelector(selector);

        Pod podTemplate = new Pod();
        State podState = new State();
        Manifest manifest = new Manifest();
        manifest.setVersion("v1beta1");
        manifest.setId(id);
        Container container = new Container();
        container.setName("nirmal-php");
        container.setImage(dockerImage);
        Port p = new Port();
        p.setContainerPort(80);
        container.setPorts(new Port[] { p });
        manifest.setContainers(new Container[] { container });
        podState.setManifest(manifest);
        podTemplate.setDesiredState(podState);
        Label l1 = new Label();
        l1.setName("nirmal");
        podTemplate.setLabels(l1);

        desiredState.setPodTemplate(podTemplate);
        contr.setDesiredState(desiredState);
        Label l2 = new Label();
        l2.setName("nirmal");
        contr.setLabels(l2);
        if (log.isDebugEnabled()) {
            log.debug("Creating a Replication Controller: "+contr);
        }
        client.createReplicationController(contr);
        assertNotNull(client.getReplicationController(id));
        
        // wait 10s for Pods to be created
        Thread.sleep(10000);
        
        // test recreation using same id
        client.createReplicationController(contr);
        assertNotNull(client.getReplicationController(id));
        
        assertEquals(1, client.getAllReplicationControllers().length);
        
        Pod[] pods = client.getSelectedPods(new Label[]{l1});
        assertEquals(replicas, pods.length);
        
        // test incorrect replica count
        replicas = -1;
        try {
            client.updateReplicationController(id, replicas);
        } catch (Exception e) {
            assertEquals(true, e instanceof KubernetesClientException);
            assertEquals(true, e.getMessage().contains("update failed"));
        }
        
        replicas = 0;
        client.updateReplicationController(id, replicas);
        
        Thread.sleep(10000);
        
        pods = client.getSelectedPods(new Label[]{l1});
        assertEquals(replicas, pods.length);
        
        client.deleteReplicationController(id);
        try {
            client.getReplicationController(id);
        } catch (Exception e) {
            assertEquals(true, e instanceof KubernetesClientException);
        }
        
        String bogusContrId = "nirmal";
        // create an invalid Controller
        ReplicationController bogusContr = new ReplicationController();
        bogusContr.setId(bogusContrId);
        try {
            client.createReplicationController(bogusContr);
        } catch (Exception e) {
            assertEquals(true, e instanceof KubernetesClientException);
        }
        
        try {
            client.getReplicationController(bogusContrId);
        } catch (Exception e) {
            assertEquals(true, e instanceof KubernetesClientException);
            assertEquals("Replication Controller ["+bogusContrId+"] doesn't exist.", e.getMessage());
        }
        
        try {
            client.updateReplicationController(bogusContrId, 3);
        } catch (Exception e) {
            assertEquals(true, e instanceof KubernetesClientException);
            assertEquals("Replication Controller ["+bogusContrId+"] doesn't exist.", e.getMessage());
        }
        
        try {
            client.deleteReplicationController(bogusContrId);
        } catch (Exception e) {
            assertEquals(true, e instanceof KubernetesClientException);
            assertEquals("Replication Controller ["+bogusContrId+"] doesn't exist.", e.getMessage());
        }
	}
	
	@Test
    public void testServices() throws Exception { 
	    String serviceId = "nirmal-service";
	    Service serv = new Service();
	    serv.setApiVersion("v1beta1");
	    serv.setContainerPort("8379");
	    serv.setPort(5000);
	    serv.setId(serviceId);
	    serv.setKind("Service");
	    
	    Label l = new Label();
	    l.setName("nirmal");
	    
	    serv.setLabels(l);
	    serv.setName("nirmal-service");
	    Selector selector = new Selector();
	    selector.setName(l.getName());
	    serv.setSelector(selector);
	    
	    if (log.isDebugEnabled()) {
            log.debug("Creating a Service Proxy: "+serv);
        }
        client.createService(serv);
        assertNotNull(client.getService(serviceId));
        
        // test recreation using same id
        client.createService(serv);
        assertNotNull(client.getService(serviceId));
        
        assertEquals(1, client.getAllServices().length);
        
        client.deleteService(serviceId);
        try {
            client.getService(serviceId);
        } catch (Exception e) {
            assertEquals(true, e instanceof KubernetesClientException);
        }
        
        String bogusServId = "nirmal";
        // create an invalid Service
        Service bogusServ = new Service();
        bogusServ.setId(bogusServId);
        try {
            client.createService(bogusServ);
        } catch (Exception e) {
            assertEquals(true, e instanceof KubernetesClientException);
        }
        
        try {
            client.getService(bogusServId);
        } catch (Exception e) {
            assertEquals(true, e instanceof KubernetesClientException);
            assertEquals("Service ["+bogusServId+"] doesn't exist.", e.getMessage());
        }
        
        try {
            client.deleteService(bogusServId);
        } catch (Exception e) {
            assertEquals(true, e instanceof KubernetesClientException);
            assertEquals("Service ["+bogusServId+"] doesn't exist.", e.getMessage());
        }
	}
}
