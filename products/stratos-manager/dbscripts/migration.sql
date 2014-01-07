/*
* Licensed to the Apache Software Foundation (ASF) under one
* or more contributor license agreements. See the NOTICE file
* distributed with this work for additional information
* regarding copyright ownership. The ASF licenses this file
* to you under the Apache License, Version 2.0 (the
* "License"); you may not use this file except in compliance
* with the License. You may obtain a copy of the License at
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing,
* software distributed under the License is distributed on an
* "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
* KIND, either express or implied. See the License for the
* specific language governing permissions and limitations
* under the License.
*/ 

ALTER TABLE UM_TENANT ADD UM_CREATED_DATE TIMESTAMP NOT NULL DEFAULT '2010-06-01 00:00:01';  
ALTER TABLE UM_TENANT ADD UNIQUE INDEX INDEX_UM_TENANT_UM_DOMAIN_NAME (UM_DOMAIN_NAME);

ALTER TABLE REG_ASSOCIATION ADD INDEX REG_ASSOCIATION_INDEX_SOURCEPATH (REG_SOURCEPATH, REG_TENANT_ID);
ALTER TABLE REG_ASSOCIATION ADD INDEX REG_ASSOCIATION_INDEX_TARGETPATH (REG_TARGETPATH, REG_TENANT_ID);
ALTER TABLE REG_ASSOCIATION ADD INDEX REG_ASSOCIATION_INDEX_ASSOCIATION_TYPE (REG_ASSOCIATION_TYPE, REG_TENANT_ID); 

