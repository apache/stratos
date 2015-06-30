/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package io.fabric8.kubernetes.api.model;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.hamcrest.CoreMatchers;
import org.junit.Test;

import static org.junit.Assert.*;

public class KubernetesListTest {

    @Test
    public void testDefaultValues() throws JsonProcessingException {
        Service service = new ServiceBuilder()
                .withNewMetadata()
                    .withName("test-service")
                .endMetadata()
                .build();
        assertNotNull(service.getApiVersion());
        assertEquals(service.getKind(), "Service");
        
        ReplicationController replicationController = new ReplicationControllerBuilder()
                .withNewMetadata()
                .withName("test-controller")
                .endMetadata()
                .build();
        assertNotNull(replicationController.getApiVersion());
        assertEquals(replicationController.getKind(), "ReplicationController");
        
        KubernetesList kubernetesList = new KubernetesListBuilder()
                .addNewServiceItem()
                .withNewMetadata()
                    .withName("test-service")
                .endMetadata()
                .and()
                .addNewReplicationControllerItem()
                .withNewMetadata()
                    .withName("test-controller")
                .endMetadata()
                .and()
                .build();
        
        assertNotNull(kubernetesList.getApiVersion());
        assertEquals(kubernetesList.getKind(), "List");
        assertThat(kubernetesList.getItems(), CoreMatchers.hasItem(service));
        assertThat(kubernetesList.getItems(), CoreMatchers.hasItem(replicationController));
    }

    @Test
    public void testVisitor() throws JsonProcessingException {
        KubernetesList list = new KubernetesListBuilder()
                .addNewPodItem()
                    .withNewSpec()
                        .addNewContainer()
                            .withName("my-container")
                            .withImage("my/image")
                        .endContainer()
                    .endSpec()
                .and()
                .build();

        list = new KubernetesListBuilder(list).accept(new io.fabric8.common.Visitor() {
            public void visit(Object item) {
                if (item instanceof io.fabric8.kubernetes.api.model.PodSpecBuilder) {
                    ((io.fabric8.kubernetes.api.model.PodSpecBuilder)item).addNewContainer()
                            .withName("other-container")
                            .withImage("other/image")
                            .and();
                }
            }
        }).build();
    }


    @Test
    public void testDefaultNullValues() throws JsonProcessingException {
        Container container = new ContainerBuilder().build();
        assertNull(container.getLifecycle());
        assertNull(container.getLivenessProbe());


        Pod pod = new PodBuilder().build();
        assertNull(pod.getSpec());
        assertNull(pod.getStatus());
    }
}