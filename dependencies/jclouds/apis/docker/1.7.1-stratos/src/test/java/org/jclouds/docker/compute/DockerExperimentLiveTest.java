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
package org.jclouds.docker.compute;

import com.beust.jcommander.internal.Maps;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableSet;
import com.google.inject.Module;
import org.jclouds.ContextBuilder;
import org.jclouds.compute.ComputeService;
import org.jclouds.compute.ComputeServiceContext;
import org.jclouds.compute.RunNodesException;
import org.jclouds.compute.domain.ExecResponse;
import org.jclouds.compute.domain.NodeMetadata;
import org.jclouds.compute.domain.OsFamily;
import org.jclouds.compute.domain.Template;
import org.jclouds.compute.reference.ComputeServiceConstants;
import org.jclouds.docker.compute.options.DockerTemplateOptions;
import org.jclouds.logging.Logger;
import org.jclouds.logging.slf4j.config.SLF4JLoggingModule;
import org.jclouds.scriptbuilder.domain.Statement;
import org.jclouds.scriptbuilder.statements.login.AdminAccess;
import org.jclouds.ssh.SshClient;
import org.jclouds.sshj.config.SshjSshClientModule;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.annotation.Resource;
import javax.inject.Named;
import java.io.IOException;
import java.util.Map;
import java.util.Set;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

/**
 * @author Andrea Turli
 */
@Test(groups = "live", singleThreaded = true, testName = "DockerExperimentLiveTest")
public class DockerExperimentLiveTest extends BaseDockerApiLiveTest {

   public static final String TEST_LAUNCH_CLUSTER = "jclouds";
   @Resource
   @Named(ComputeServiceConstants.COMPUTE_LOGGER)
   protected Logger logger = Logger.NULL;

   ComputeServiceContext context;

   @BeforeMethod
   public void setUp() throws IOException {
      context = ContextBuilder.newBuilder("docker")
              .overrides(super.setupProperties())
              .modules(ImmutableSet.<Module>of(new SLF4JLoggingModule(), new SshjSshClientModule()))
              .build(ComputeServiceContext.class);
   }

   @AfterMethod
   public void tearDown() {
      context.close();
   }

   @Test
   public void testLaunchUbuntuServerWithInboundPorts() throws RunNodesException {
      int numNodes = 1;
      ComputeService compute = context.getComputeService();

      Template template = compute.templateBuilder().smallest()
              .osFamily(OsFamily.UBUNTU).os64Bit(true)
              .osDescriptionMatches("jclouds/ubuntu:latest")
              .build();
      Statement bootInstructions = AdminAccess.standard();

      DockerTemplateOptions templateOptions = template.getOptions().as(DockerTemplateOptions.class);

      Map<String,String> volumes = Maps.newHashMap();
      volumes.put("/var/lib/docker", "/root");
      templateOptions.volumes(volumes).runScript(bootInstructions)
              .inboundPorts(22, 80, 8080);

      Set<? extends NodeMetadata> nodes = context.getComputeService().createNodesInGroup(TEST_LAUNCH_CLUSTER, numNodes, template);
      assertEquals(numNodes, nodes.size(), "wrong number of nodes");
      for (NodeMetadata node : nodes) {
         assertTrue(node.getGroup().equals(TEST_LAUNCH_CLUSTER));
         logger.debug("Created Node: %s", node);
         SshClient client = context.utils().sshForNode().apply(node);
         client.connect();
         ExecResponse hello = client.exec("echo hello");
         assertEquals(hello.getOutput().trim(), "hello");
      }
      context.getComputeService().destroyNodesMatching(new Predicate<NodeMetadata>() {
         @Override
         public boolean apply(NodeMetadata input) {
            return input.getGroup().contains(TEST_LAUNCH_CLUSTER);
         }
      });
   }

   public void testLaunchUbuntuCluster() throws RunNodesException {
      int numNodes = 1;
      ComputeService compute = context.getComputeService();
      Template template = compute.templateBuilder()
              .smallest().osFamily(OsFamily.UBUNTU)
              .os64Bit(true)
              .osDescriptionMatches("jclouds/ubuntu:latest")
              .build();
      Statement bootInstructions = AdminAccess.standard();
      template.getOptions().runScript(bootInstructions)
              .inboundPorts(22);

      Set<? extends NodeMetadata> nodes = context.getComputeService().createNodesInGroup(TEST_LAUNCH_CLUSTER, numNodes, template);
      assertEquals(numNodes, nodes.size(), "wrong number of nodes");
      for (NodeMetadata node : nodes) {
         assertTrue(node.getGroup().equals(TEST_LAUNCH_CLUSTER));
         logger.debug("Created Node: %s", node);
         SshClient client = context.utils().sshForNode().apply(node);
         client.connect();
         ExecResponse hello = client.exec("echo hello");
         assertEquals(hello.getOutput().trim(), "hello");
      }
      context.getComputeService().destroyNodesMatching(new Predicate<NodeMetadata>() {
         @Override
         public boolean apply(NodeMetadata input) {
            return input.getGroup().contains(TEST_LAUNCH_CLUSTER);
         }
      });
   }

}
