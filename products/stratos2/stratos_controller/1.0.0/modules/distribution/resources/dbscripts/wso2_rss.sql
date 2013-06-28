
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
