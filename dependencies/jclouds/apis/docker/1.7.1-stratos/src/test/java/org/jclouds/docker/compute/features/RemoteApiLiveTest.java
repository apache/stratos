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

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.io.Resources;
import org.jclouds.docker.compute.BaseDockerApiLiveTest;
import org.jclouds.docker.domain.Config;
import org.jclouds.docker.domain.Container;
import org.jclouds.docker.domain.Image;
import org.jclouds.docker.options.BuildOptions;
import org.jclouds.docker.options.CreateImageOptions;
import org.jclouds.docker.options.DeleteImageOptions;
import org.jclouds.rest.ResourceNotFoundException;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;

import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNull;

/**
 * @author Andrea Turli
 */
public class RemoteApiLiveTest extends BaseDockerApiLiveTest {

   private static final String BUSYBOX_IMAGE = "busybox";
   private Container container = null;
   private Image image = null;

   @BeforeClass
   private void init() {
      setupProperties();
      CreateImageOptions options = CreateImageOptions.Builder.fromImage(BUSYBOX_IMAGE);
      InputStream createImageStream = api().createImage(options);
      consumeStream(createImageStream, false);
   }

   @Test
   public void testVersion() {
      Assert.assertEquals(api().getVersion().getVersion(), "0.9.0");
   }

   @Test(dependsOnMethods = "testVersion")
   public void testCreateImage() throws IOException, InterruptedException {
      CreateImageOptions options = CreateImageOptions.Builder.fromImage(BUSYBOX_IMAGE);
      InputStream createImageStream = api().createImage(options);
      consumeStream(createImageStream, false);
      image = api().inspectImage(BUSYBOX_IMAGE);
      assertNotNull(image);
   }

   @Test(dependsOnMethods = "testCreateImage")
   public void testListImages() {
      Assert.assertNotNull(api().listImages());
   }

   @Test(dependsOnMethods = "testListImages")
   public void testCreateContainer() throws IOException, InterruptedException {
      if(image == null) Assert.fail();
      Config config = Config.builder().imageId(image.getId())
              .cmd(ImmutableList.of("/bin/sh", "-c", "while true; do echo hello world; sleep 1; done"))
              .build();
      container = api().createContainer("testCreateContainer", config);
      assertNotNull(container);
      assertNotNull(container.getId());
   }

   @Test(dependsOnMethods = "testCreateContainer")
   public void testStartContainer() throws IOException, InterruptedException {
      if(container == null) Assert.fail();
      api().startContainer(container.getId());
      Assert.assertTrue(api().inspectContainer(container.getId()).getState().isRunning());
   }

   @Test(dependsOnMethods = "testStartContainer")
   public void testStopContainer() {
      if(container == null) Assert.fail();
      api().stopContainer(container.getId());
      Assert.assertFalse(api().inspectContainer(container.getId()).getState().isRunning());
   }

   @Test(dependsOnMethods = "testStopContainer", expectedExceptions = NullPointerException.class)
   public void testRemoveContainer() {
      if(container == null) Assert.fail();
      api().removeContainer(container.getId());
      Assert.assertFalse(api().inspectContainer(container.getId()).getState().isRunning());
   }

   @Test(dependsOnMethods = "testRemoveContainer", expectedExceptions = ResourceNotFoundException.class)
   public void testDeleteImage() {
      InputStream deleteImageStream = api().deleteImage(image.getId());
      consumeStream(deleteImageStream, false);
      assertNull(api().inspectImage(image.getId()));
   }

   @Test(dependsOnMethods = "testDeleteImage")
   public void testBuildImage() throws IOException, InterruptedException, URISyntaxException {
      BuildOptions options = BuildOptions.Builder.tag("testBuildImage").verbose(false).nocache(false);
      InputStream buildImageStream = api().build(new File(Resources.getResource("centos/Dockerfile").toURI()), options);
      String buildStream = consumeStream(buildImageStream, false);
      Iterable<String> splitted = Splitter.on("\n").split(buildStream.replace("\r", "").trim());
      String lastStreamedLine = Iterables.getLast(splitted).trim();
      String rawImageId = Iterables.getLast(Splitter.on("Successfully built ").split(lastStreamedLine));
      String imageId = rawImageId.substring(0, 11);
      Image image = api().inspectImage(imageId);
      api().removeContainer(image.getContainer());
      api().deleteImage(imageId, DeleteImageOptions.Builder.force(true));
   }

   private RemoteApi api() {
      return api.getRemoteApi();
   }

}
