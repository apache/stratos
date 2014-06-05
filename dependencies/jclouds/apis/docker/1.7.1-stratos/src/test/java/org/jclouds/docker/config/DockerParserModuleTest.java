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
package org.jclouds.docker.config;

import static org.jclouds.docker.config.DockerParserModule.ContainerTypeAdapter;
import static org.jclouds.docker.config.DockerParserModule.ImageTypeAdapter;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;

import org.jclouds.docker.domain.Container;
import org.jclouds.docker.domain.Image;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 * Unit tests for the {@link org.jclouds.docker.config.DockerParserModule} class.
 *
 * @author Andrea Turli
 */
@Test(groups = "unit", testName = "DockerParserModuleTest")
public class DockerParserModuleTest {

   private Gson gson;

   @BeforeMethod
   public void setup() {
      gson = new GsonBuilder()
              .registerTypeAdapter(Container.class, new ContainerTypeAdapter())
              .registerTypeAdapter(Image.class, new ImageTypeAdapter())
              .create();
   }

   @Test
   public void testContainerID() {
      Container container = gson.fromJson(
              "{ \"ID\": \"1111111111111111111111111111111111111111111111111111111111111111\" }",
              Container.class);
      assertNotNull(container);
      assertEquals(container.getId(), "1111111111111111111111111111111111111111111111111111111111111111");
   }


   @Test
   public void testContainerId() {
      Container container = gson.fromJson(
              "{ \"Id\": \"2222222222222222222222222222222222222222222222222222222222222222\" }",
              Container.class);
      assertNotNull(container);
      assertEquals(container.getId(), "2222222222222222222222222222222222222222222222222222222222222222");
   }

   @Test
   public void testContainerName() {
      Container container = gson.fromJson(
              "{ \"Name\": \"example\" }",
              Container.class);
      assertNotNull(container);
      assertEquals(container.getName(), "example");
   }


   @Test
   public void testContainerNames() {
      Container container = gson.fromJson("{ \"Names\": [\"/jclouds-b45\"] }", Container.class);
      assertNotNull(container);
      assertEquals(container.getName(), "/jclouds-b45");
   }

   @Test
   public void testImageid() {
      Image image = gson.fromJson(
              "{ \"id\": \"3333333333333333333333333333333333333333333333333333333333333333\" }", Image.class);
      assertNotNull(image);
      assertEquals(image.getId(), "3333333333333333333333333333333333333333333333333333333333333333");
   }


   @Test
   public void testImageId() {
      Image image = gson.fromJson(
              "{ \"Id\": \"4444444444444444444444444444444444444444444444444444444444444444\" }", Image.class);
      assertNotNull(image);
      assertEquals(image.getId(), "4444444444444444444444444444444444444444444444444444444444444444");
   }
}
