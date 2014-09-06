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
package org.jclouds.vcloud.functions;

import static com.google.common.collect.Iterables.transform;

import javax.annotation.Resource;
import javax.inject.Inject;
import javax.inject.Singleton;

import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import com.google.common.collect.ImmutableSet;
import org.jclouds.logging.Logger;
import org.jclouds.vcloud.VCloudApi;
import org.jclouds.vcloud.domain.Catalog;
import org.jclouds.vcloud.domain.Org;
import org.jclouds.vcloud.domain.ReferenceType;

import com.google.common.base.Function;

import java.util.Collection;

@Singleton
public class CatalogsInOrg implements Function<Org, Iterable<Catalog>> {
   @Resource
   public Logger logger = Logger.NULL;

   private final VCloudApi aclient;

   @Inject
   CatalogsInOrg(VCloudApi aclient) {
      this.aclient = aclient;
   }

   @Override
   public Iterable<Catalog> apply(final Org org) {
       Collection<ReferenceType> filtered = Collections2.filter(
               org.getCatalogs().values(), new Predicate<ReferenceType>() {
                   @Override
                   public boolean apply(ReferenceType type) {
                       if (type == null) {
                           return false;
                       }
                       return !ImmutableSet.of("add", "remove").contains(type.getRelationship());
                   }
               });

       return transform(filtered, new Function<ReferenceType, Catalog>() {
         public Catalog apply(ReferenceType from) {
            return aclient.getCatalogApi().getCatalog(from.getHref());
         }
      });
   }
}
