/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jclouds.docker.compute.features;

import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.io.Files;
import com.squareup.okhttp.mockwebserver.MockResponse;
import com.squareup.okhttp.mockwebserver.MockWebServer;
import org.jclouds.docker.DockerApi;
import org.jclouds.docker.domain.Config;
import org.jclouds.docker.domain.Container;
import org.jclouds.docker.internal.BaseDockerMockTest;
import org.jclouds.docker.options.BuildOptions;
import org.jclouds.docker.options.CreateImageOptions;
import org.jclouds.docker.options.ListContainerOptions;
import org.jclouds.rest.ResourceNotFoundException;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.util.Set;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNull;
import static org.testng.AssertJUnit.fail;

/**
 * Mock tests for the {@link org.jclouds.docker.DockerApi} class.
 * 
 * @author Andrea Turli
 */
@Test(groups = "unit", testName = "RemoteApiMockTest")
public class RemoteApiMockTest extends BaseDockerMockTest {

   public void testListContainers() throws Exception {
      MockWebServer server = mockWebServer();
      server.enqueue(new MockResponse().setBody(payloadFromResource("/containers.json")));

      DockerApi api = api(server.getUrl("/"));
      RemoteApi remoteApi = api.getRemoteApi();

      try {
         Set<Container> containers = remoteApi.listContainers();
         assertRequestHasCommonFields(server.takeRequest(), "/containers/json");
         assertEquals(containers.size(), 1);
      } finally {
         api.close();
         server.shutdown();
      }
   }

   public void testListAllContainers() throws Exception {
      MockWebServer server = mockWebServer();
      server.enqueue(new MockResponse().setBody(payloadFromResource("/containers.json")));

      DockerApi api = api(server.getUrl("/"));
      RemoteApi remoteApi = api.getRemoteApi();

      try {
         Set<Container> containers = remoteApi.listContainers(ListContainerOptions.Builder.all(true));
         assertRequestHasParameters(server.takeRequest(), "/containers/json", ImmutableMultimap.of("all",
                 "true"));
         assertEquals(containers.size(), 1);
      } finally {
         api.close();
         server.shutdown();
      }
   }

   public void testGetContainer() throws Exception {
      MockWebServer server = mockWebServer();
      server.enqueue(new MockResponse().setBody(payloadFromResource("/container.json")));
      DockerApi api = api(server.getUrl("/"));
      RemoteApi remoteApi = api.getRemoteApi();
      String containerId = "6d35806c1bd2b25cd92bba2d2c2c5169dc2156f53ab45c2b62d76e2d2fee14a9";
      try {
         Container container = remoteApi.inspectContainer(containerId);
         assertRequestHasCommonFields(server.takeRequest(), "/containers/" + containerId + "/json");
         assertNotNull(container);
         assertNotNull(container.getConfig());
         assertNotNull(container.getHostConfig());
         assertEquals(container.getName(), "/hopeful_mclean");
         assertEquals(container.getState().isRunning(), true);
      } finally {
         api.close();
         server.shutdown();
      }
   }

   public void testGetNonExistingContainer() throws Exception {
      MockWebServer server = mockWebServer();
      server.enqueue(new MockResponse().setResponseCode(404));
      DockerApi api = api(server.getUrl("/"));
      RemoteApi remoteApi = api.getRemoteApi();
      String containerId = "notExisting";
      try {
         Container container = remoteApi.inspectContainer(containerId);
         assertRequestHasCommonFields(server.takeRequest(), "/containers/" + containerId + "/json");
         assertNull(container);
      } finally {
         api.close();
         server.shutdown();
      }
   }

   public void testCreateContainer() throws Exception {
      MockWebServer server = mockWebServer();
      server.enqueue(new MockResponse().setBody(payloadFromResource("/container-creation.json")));

      DockerApi api = api(server.getUrl("/"));
      RemoteApi remoteApi = api.getRemoteApi();
      Config config = Config.builder().cmd(ImmutableList.of("date"))
              .attachStdin(false)
              .attachStderr(true)
              .attachStdout(true)
              .tty(false)
              .imageId("base")
              .build();
      try {
         Container container = remoteApi.createContainer("test", config);
         assertNotNull(container);
         assertEquals(container.getId(), "c6c74153ae4b1d1633d68890a68d89c40aa5e284a1ea016cbc6ef0e634ee37b2");
      } finally {
         api.close();
         server.shutdown();
      }
   }

   public void testRemoveContainer() throws Exception {
      MockWebServer server = mockWebServer();
      server.enqueue(new MockResponse().setResponseCode(204));

      DockerApi api = api(server.getUrl("/"));
      RemoteApi remoteApi = api.getRemoteApi();
      String containerId = "6d35806c1bd2b25cd92bba2d2c2c5169dc2156f53ab45c2b62d76e2d2fee14a9";

      try {
         remoteApi.removeContainer(containerId);
         assertRequestHasCommonFields(server.takeRequest(), "DELETE", "/containers/"+containerId);
      } finally {
         api.close();
         server.shutdown();
      }
   }

   public void testRemoveNonExistingContainer() throws Exception {
      MockWebServer server = mockWebServer();
      server.enqueue(new MockResponse().setResponseCode(404));
      DockerApi api = api(server.getUrl("/"));
      RemoteApi remoteApi = api.getRemoteApi();
      String containerId = "nonExisting";
      try {
         remoteApi.removeContainer(containerId);
      } catch (ResourceNotFoundException ex) {
         // Expected exception
         assertRequestHasCommonFields(server.takeRequest(), "DELETE", "/containers/"+containerId);
      } finally {
         api.close();
         server.shutdown();
      }
   }

   public void testStartContainer() throws Exception {
      MockWebServer server = mockWebServer();
      server.enqueue(new MockResponse().setResponseCode(200));
      DockerApi api = api(server.getUrl("/"));
      RemoteApi remoteApi = api.getRemoteApi();
      try {
         remoteApi.startContainer("1");
         assertRequestHasCommonFields(server.takeRequest(), "POST", "/containers/1/start");
      } finally {
         api.close();
         server.shutdown();
      }
   }

   public void testStartNonExistingContainer() throws Exception {
      MockWebServer server = mockWebServer();
      server.enqueue(new MockResponse().setResponseCode(204));
      DockerApi api = api(server.getUrl("/"));
      RemoteApi remoteApi = api.getRemoteApi();
      try {
         try {
            remoteApi.startContainer("1");
         } catch (ResourceNotFoundException ex) {
            // Expected exception
         }
         assertRequestHasCommonFields(server.takeRequest(), "POST", "/containers/1/start");
      } finally {
         api.close();
         server.shutdown();
      }
   }

   public void testStopContainer() throws Exception {
      MockWebServer server = mockWebServer();
      server.enqueue(new MockResponse().setResponseCode(200));
      DockerApi api = api(server.getUrl("/"));
      RemoteApi remoteApi = api.getRemoteApi();
      try {
         remoteApi.stopContainer("1");
         assertRequestHasCommonFields(server.takeRequest(), "POST", "/containers/1/stop");
      } finally {
         api.close();
         server.shutdown();
      }
   }

   public void testStopNonExistingContainer() throws Exception {
      MockWebServer server = mockWebServer();
      server.enqueue(new MockResponse().setResponseCode(204));
      DockerApi api = api(server.getUrl("/"));
      RemoteApi remoteApi = api.getRemoteApi();
      try {
         remoteApi.stopContainer("1");
      } catch (ResourceNotFoundException ex) {
         // Expected exception
         assertRequestHasCommonFields(server.takeRequest(), "POST", "/containers/1/stop");
      } finally {
         api.close();
         server.shutdown();
      }
   }

   public void testCreateImage() throws Exception {
      MockWebServer server = mockWebServer();
      server.enqueue(new MockResponse().setResponseCode(200));
      DockerApi api = api(server.getUrl("/"));
      RemoteApi remoteApi = api.getRemoteApi();
      try {
         remoteApi.createImage(CreateImageOptions.Builder.fromImage("base"));
         assertRequestHasParameters(server.takeRequest(), "POST", "/images/create", ImmutableMultimap.of("fromImage",
                 "base"));
      } finally {
         api.close();
         server.shutdown();
      }
   }

   public void testCreateImageFailure() throws Exception {
      MockWebServer server = mockWebServer();
      server.enqueue(new MockResponse().setResponseCode(404));
      DockerApi api = api(server.getUrl("/"));
      RemoteApi remoteApi = api.getRemoteApi();
      try {
         remoteApi.createImage(CreateImageOptions.Builder.fromImage("base"));
         assertRequestHasParameters(server.takeRequest(), "POST", "/images/create", ImmutableMultimap.of("fromImage",
                 "base"));
      } catch (ResourceNotFoundException ex) {
         // Expected exception
         assertRequestHasParameters(server.takeRequest(), "POST", "/images/create", ImmutableMultimap.of("fromImage",
                 "base"));
      } finally {
         api.close();
         server.shutdown();
      }
   }

   public void testDeleteImage() throws Exception {
      MockWebServer server = mockWebServer();
      server.enqueue(new MockResponse().setResponseCode(204));
      DockerApi api = api(server.getUrl("/"));
      RemoteApi remoteApi = api.getRemoteApi();
      try {
         remoteApi.deleteImage("1");
         assertRequestHasCommonFields(server.takeRequest(), "DELETE", "/images/1");
      } finally {
         api.close();
         server.shutdown();
      }
   }

   public void testDeleteNotExistingImage() throws Exception {
      MockWebServer server = mockWebServer();
      server.enqueue(new MockResponse().setResponseCode(404));
      DockerApi api = api(server.getUrl("/"));
      RemoteApi remoteApi = api.getRemoteApi();
      try {
         remoteApi.deleteImage("1");
      } catch (ResourceNotFoundException ex) {
         // Expected exception
         assertRequestHasCommonFields(server.takeRequest(), "DELETE", "/images/1");
      } finally {
         api.close();
         server.shutdown();
      }
   }

   public void testBuildContainer() throws Exception {
      MockWebServer server = mockWebServer();
      server.enqueue(new MockResponse().setResponseCode(200));
      DockerApi api = api(server.getUrl("/"));
      RemoteApi remoteApi = api.getRemoteApi();
      String content = new String(payloadFromResource("/Dockerfile"));
      File dockerFile = createDockerFile(content);
      try {
         remoteApi.build(dockerFile, BuildOptions.NONE);
         assertRequestHasCommonFields(server.takeRequest(), "POST", "/build");
      } finally {
         dockerFile.deleteOnExit();
         api.close();
         server.shutdown();
      }
   }

   public void testBuildNonexistentContainer() throws Exception {
      MockWebServer server = mockWebServer();
      server.enqueue(new MockResponse().setResponseCode(404));

      DockerApi api = api(server.getUrl("/"));
      RemoteApi remoteApi = api.getRemoteApi();

      String content = new String(payloadFromResource("/Dockerfile"));
      File dockerFile = createDockerFile(content);
      try {
         try {
            remoteApi.build(dockerFile, BuildOptions.NONE);
            fail("Build container should fail on 404");
         } catch (ResourceNotFoundException ex) {
            // Expected exception
         }
         assertRequestHasCommonFields(server.takeRequest(), "POST", "/build");
      } finally {
         dockerFile.deleteOnExit();
         api.close();
         server.shutdown();
      }
   }

   private File createDockerFile(String content) {
      File newTempDir = Files.createTempDir();
      File dockerFile = new File(newTempDir + "/dockerFile");
      try {
         Files.write(content, dockerFile, Charsets.UTF_8);
      } catch(IOException e) {
         Assert.fail();
      }
      return dockerFile;
   }
}
