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
package org.jclouds.docker.compute.extensions;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static org.jclouds.compute.config.ComputeServiceProperties.TIMEOUT_IMAGE_AVAILABLE;
import java.util.NoSuchElementException;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicReference;

import javax.annotation.Resource;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.jclouds.Constants;
import org.jclouds.compute.domain.CloneImageTemplate;
import org.jclouds.compute.domain.Image;
import org.jclouds.compute.domain.ImageTemplate;
import org.jclouds.compute.domain.ImageTemplateBuilder;
import org.jclouds.compute.extensions.ImageExtension;
import org.jclouds.compute.reference.ComputeServiceConstants;
import org.jclouds.docker.DockerApi;
import org.jclouds.docker.compute.functions.ImageToImage;
import org.jclouds.docker.domain.Container;
import org.jclouds.docker.options.CommitOptions;
import org.jclouds.logging.Logger;

import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.Atomics;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.UncheckedTimeoutException;

/**
 * Docker implementation of {@link org.jclouds.compute.extensions.ImageExtension}
 *
 * @author Andrea Turli
 */
@Singleton
public class DockerImageExtension implements ImageExtension {

   @Resource
   @Named(ComputeServiceConstants.COMPUTE_LOGGER)
   private Logger logger = Logger.NULL;
   private final DockerApi api;
   private final ListeningExecutorService userExecutor;
   private final Predicate<AtomicReference<Image>> imageAvailablePredicate;

   @Inject
   public DockerImageExtension(DockerApi api, @Named(Constants.PROPERTY_USER_THREADS) ListeningExecutorService
           userExecutor, @Named(TIMEOUT_IMAGE_AVAILABLE) Predicate<AtomicReference<Image>> imageAvailablePredicate) {
      this.api = checkNotNull(api, "api");
      this.userExecutor = checkNotNull(userExecutor, "userExecutor");
      this.imageAvailablePredicate = checkNotNull(imageAvailablePredicate, "imageAvailablePredicate");
   }

   @Override
   public ImageTemplate buildImageTemplateFromNode(String name, final String id) {
      Container container = api.getRemoteApi().inspectContainer(id);
      if (container == null)
         throw new NoSuchElementException("Cannot find container with id: " + id);
      CloneImageTemplate template = new ImageTemplateBuilder.CloneImageTemplateBuilder().nodeId(id).name(name).build();
      return template;
   }

   @Override
   public ListenableFuture<Image> createImage(ImageTemplate template) {
      checkArgument(template instanceof CloneImageTemplate,
              " docker only currently supports creating images through cloning.");
      CloneImageTemplate cloneTemplate = (CloneImageTemplate) template;

      Container container = api.getRemoteApi().inspectContainer(cloneTemplate.getSourceNodeId());
      CommitOptions options = CommitOptions.Builder.containerId(container.getId()).tag(cloneTemplate.getName());
      org.jclouds.docker.domain.Image dockerImage = api.getRemoteApi().commit(options);

      dockerImage = org.jclouds.docker.domain.Image.builder().fromImage(dockerImage)
              .repoTags(ImmutableList.of(cloneTemplate.getName() + ":latest"))
              .build();

      logger.info(">> Registered new image %s, waiting for it to become available.", dockerImage.getId());
      final AtomicReference<Image> image = Atomics.newReference(new ImageToImage().apply(dockerImage));
      return userExecutor.submit(new Callable<Image>() {
         @Override
         public Image call() throws Exception {
            if (imageAvailablePredicate.apply(image))
               return image.get();
            throw new UncheckedTimeoutException("Image was not created within the time limit: " + image.get());
         }
      });
   }

   @Override
   public boolean deleteImage(String id) {
      try {
         api.getRemoteApi().deleteImage(id);
      } catch (Exception e) {
         logger.error(e, "Could not delete image with id %s ", id);
         return false;
      }
      return true;
   }

}
