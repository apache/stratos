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

CREATE TABLE RSS_INSTANCE (
  rss_instance_id INTEGER AUTO_INCREMENT,
  name VARCHAR(128) NOT NULL,
  server_url VARCHAR(1024) NOT NULL,
  dbms_type VARCHAR(128) NOT NULL,
  instance_type VARCHAR(128) NOT NULL,
  server_category VARCHAR(128) NOT NULL,
  admin_username VARCHAR(128),
  admin_password VARCHAR(128),
  tenant_id INTEGER NOT NULL,
  UNIQUE (name, tenant_id),
  PRIMARY KEY (rss_instance_id)
) ENGINE="InnoDB";

CREATE TABLE DATABASE_INSTANCE (
  database_instance_id INTEGER AUTO_INCREMENT,
  name VARCHAR(128) NOT NULL,
  rss_instance_id INTEGER,
  tenant_id INTEGER,
  UNIQUE (name, rss_instance_id),
  PRIMARY KEY (database_instance_id),
  FOREIGN KEY (rss_instance_id) REFERENCES RSS_INSTANCE (rss_instance_id)
) ENGINE="InnoDB";

CREATE TABLE DATABASE_USER (
  user_id INTEGER AUTO_INCREMENT,
  db_username VARCHAR(128) NOT NULL,
  rss_instance_id INTEGER,
  user_tenant_id INTEGER,
  UNIQUE (db_username, rss_instance_id, user_tenant_id),
  PRIMARY KEY (user_id),
  FOREIGN KEY (rss_instance_id) REFERENCES RSS_INSTANCE (rss_instance_id)
) ENGINE="InnoDB";

CREATE TABLE DATABASE_INSTANCE_PROPERTY (
  db_property_id INTEGER AUTO_INCREMENT,
  prop_name VARCHAR(128) NOT NULL,
  prop_value TEXT,
  database_instance_id INTEGER,
  UNIQUE (prop_name, database_instance_id),
  PRIMARY KEY (db_property_id),
  FOREIGN KEY (database_instance_id) REFERENCES DATABASE_INSTANCE (database_instance_id)
) ENGINE="InnoDB";

CREATE TABLE USER_DATABASE_ENTRY (
  user_id INTEGER,
  database_instance_id INTEGER,
  PRIMARY KEY (user_id, database_instance_id),
  FOREIGN KEY (user_id) REFERENCES DATABASE_USER (user_id),
  FOREIGN KEY (database_instance_id) REFERENCES DATABASE_INSTANCE (database_instance_id)
) ENGINE="InnoDB";

CREATE TABLE USER_DATABASE_PERMISSION (
  user_id INTEGER,
  database_instance_id INTEGER,
  perm_name VARCHAR(128) NOT NULL,
  perm_value VARCHAR(128),
  PRIMARY KEY (user_id, database_instance_id, perm_name),
  FOREIGN KEY (user_id) REFERENCES DATABASE_USER (user_id),
  FOREIGN KEY (database_instance_id) REFERENCES DATABASE_INSTANCE (database_instance_id)
) ENGINE="InnoDB";

CREATE TABLE WSO2_RSS_DATABASE_INSTANCE_COUNT (
  instance_count INTEGER NOT NULL DEFAULT 0
) ENGINE="InnoDB";

CREATE TABLE USER_PRIVILEGE_GROUP (
  priv_group_id INTEGER AUTO_INCREMENT,
  priv_group_name VARCHAR(128),
  tenant_id INTEGER,
  PRIMARY KEY (priv_group_id, priv_group_name, tenant_id)
) ENGINE="InnoDB";

CREATE TABLE USER_PRIVILEGE_GROUP_ENTRY (
  priv_group_id INTEGER,
  perm_name VARCHAR(128),
  perm_value CHAR(1),
  PRIMARY KEY (priv_group_id, perm_name),
  FOREIGN KEY (priv_group_id) REFERENCES USER_PRIVILEGE_GROUP (priv_group_id)
) ENGINE="InnoDB";
