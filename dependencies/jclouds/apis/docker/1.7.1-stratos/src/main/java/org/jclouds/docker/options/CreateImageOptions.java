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
package org.jclouds.docker.options;

import org.jclouds.http.options.BaseHttpRequestOptions;

/**
 * Options to customize container creation.
 * 
 * @author Andrea Turli
 */
public class CreateImageOptions extends BaseHttpRequestOptions {

   /*
   private final String fromImage;
   private final String fromSrc;
   private final String repo;
   private final String tag;
   private final String registry;

   public CreateImageOptions(String fromImage, String fromSrc, String repo, String tag, String registry) {
      this.fromImage = fromImage;
      this.fromSrc = fromSrc;
      this.repo = repo;
      this.tag = tag;
      this.registry = registry;

      if (fromImage != null) {
         queryParameters.put("fromImage", fromImage);
      }
      if (fromSrc != null) {
         queryParameters.put("fromSrc", fromSrc);
      }
      if (repo != null) {
         queryParameters.put("repo", repo);
      }
      if (tag != null) {
         queryParameters.put("tag", tag);
      }
      if (registry != null) {
         queryParameters.put("registry", registry);
      }
   }
   */

   public static final CreateImageOptions NONE = new CreateImageOptions();

   public CreateImageOptions fromImage(String fromImage) {
      this.queryParameters.put("fromImage", fromImage);
      return this;
   }

   public CreateImageOptions fromSrc(String fromSrc) {
      this.queryParameters.put("fromSrc", fromSrc);
      return this;
   }

   public CreateImageOptions repo(String repo) {
      this.queryParameters.put("repo", repo);
      return this;
   }

   public CreateImageOptions tag(String tag) {
      this.queryParameters.put("tag", tag);
      return this;
   }

   public CreateImageOptions registry(String registry) {
      this.queryParameters.put("registry", registry);
      return this;
   }

   public static class Builder {
      /**
       * @see CreateImageOptions#fromImage
       */
      public static CreateImageOptions fromImage(String fromImage) {
         CreateImageOptions options = new CreateImageOptions();
         return options.fromImage(fromImage);
      }

      /**
       * @see CreateImageOptions#fromSrc
       */
      public static CreateImageOptions fromSrc(String fromSrc) {
         CreateImageOptions options = new CreateImageOptions();
         return options.fromSrc(fromSrc);
      }

      /**
       * @see CreateImageOptions#repo
       */
      public static CreateImageOptions repo(String repo) {
         CreateImageOptions options = new CreateImageOptions();
         return options.repo(repo);
      }

      /**
       * @see CreateImageOptions#tag
       */
      public static CreateImageOptions tag(String tag) {
         CreateImageOptions options = new CreateImageOptions();
         return options.tag(tag);
      }

      /**
       * @see CreateImageOptions#registry
       */
      public static CreateImageOptions registry(String registry) {
         CreateImageOptions options = new CreateImageOptions();
         return options.registry(registry);
      }

   }
}
