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

-- MySQL Administrator dump 1.4
--
-- ------------------------------------------------------
-- Server version	5.5.24-0ubuntu0.12.04.1


/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8 */;

/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;


--
-- Create schema stratos_foundation
--

CREATE DATABASE IF NOT EXISTS stratos_foundation;
USE stratos_foundation;

--
-- Definition of table `stratos_foundation`.`CARTRIDGE_INSTANCE`
--

DROP TABLE IF EXISTS `stratos_foundation`.`CARTRIDGE_INSTANCE`;
CREATE TABLE  `stratos_foundation`.`CARTRIDGE_INSTANCE` (
  `ID` int(11) NOT NULL AUTO_INCREMENT,
  `INSTANCE_IP` varchar(255) NOT NULL,
  `TENANT_ID` int(11) DEFAULT NULL,
  `TENANT_DOMAIN` varchar(255) DEFAULT NULL,
  `CARTRIDGE_TYPE` varchar(255) NOT NULL,
  `STATE` varchar(255) NOT NULL,
  `CLUSTER_DOMAIN` varchar(255) NOT NULL,
  `CLUSTER_SUBDOMAIN` varchar(255) NOT NULL,
  PRIMARY KEY (`ID`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=latin1;

--
-- Definition of table `stratos_foundation`.`CARTRIDGE_SUBSCRIPTION`
--

DROP TABLE IF EXISTS `stratos_foundation`.`CARTRIDGE_SUBSCRIPTION`;
CREATE TABLE  `stratos_foundation`.`CARTRIDGE_SUBSCRIPTION` (
  `SUBSCRIPTION_ID` int(11) NOT NULL AUTO_INCREMENT,
  `TENANT_ID` int(11) NOT NULL,
  `CARTRIDGE` varchar(30) NOT NULL,
  `PROVIDER` varchar(30) NOT NULL,
  `HOSTNAME` varchar(255) NOT NULL,
  `POLICY` varchar(50) NULL,
  `CLUSTER_DOMAIN` varchar(255) NOT NULL,
  `CLUSTER_SUBDOMAIN` varchar(255) NOT NULL,
  `MGT_DOMAIN` varchar(255) NOT NULL,
  `MGT_SUBDOMAIN` varchar(255) NOT NULL,
  `STATE` varchar(30) NOT NULL,
  `ALIAS` varchar(255) NOT NULL,
  `TENANT_DOMAIN` varchar(255) NOT NULL,
  `BASE_DIR` varchar(255) NOT NULL,
  `REPO_ID` int(11) DEFAULT NULL,
  `DATA_CARTRIDGE_ID` int(11) DEFAULT NULL,
  `MAPPED_DOMAIN` varchar(255),
  `SUBSCRIPTION_KEY` varchar(255) NOT NULL,
  PRIMARY KEY (`SUBSCRIPTION_ID`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=latin1;



--
-- Definition of table `stratos_foundation`.`DATA_CARTRIDGE`
--

DROP TABLE IF EXISTS `stratos_foundation`.`DATA_CARTRIDGE`;
CREATE TABLE  `stratos_foundation`.`DATA_CARTRIDGE` (
  `DATA_CART_ID` int(11) NOT NULL AUTO_INCREMENT,
  `TYPE` varchar(30) NOT NULL,
  `USER_NAME` varchar(255) NOT NULL,
  `PASSWORD` varchar(255) NOT NULL,
  `STATE` varchar(255) NOT NULL,
  PRIMARY KEY (`DATA_CART_ID`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=latin1;


--
-- Definition of table `stratos_foundation`.`PORT_MAPPING`
--

DROP TABLE IF EXISTS `stratos_foundation`.`PORT_MAPPING`;
CREATE TABLE  `stratos_foundation`.`PORT_MAPPING` (
  `PORT_MAPPING_ID` int(11) NOT NULL AUTO_INCREMENT,
  `SUBSCRIPTION_ID` int(11) NOT NULL,
  `TYPE` varchar(30) NOT NULL,
  `PRIMARY_PORT` varchar(30) NOT NULL,
  `PROXY_PORT` varchar(30) NOT NULL,
  `STATE` varchar(30) NOT NULL,
  PRIMARY KEY (`PORT_MAPPING_ID`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=latin1;


--
-- Definition of table `stratos_foundation`.`REPOSITORY`
--

DROP TABLE IF EXISTS `stratos_foundation`.`REPOSITORY`;
CREATE TABLE  `stratos_foundation`.`REPOSITORY` (
  `REPO_ID` int(11) NOT NULL AUTO_INCREMENT,
  `REPO_NAME` varchar(255) NOT NULL,
  `STATE` varchar(30) NOT NULL,
  `REPO_USER_NAME` varchar(255) NOT NULL,
  `REPO_USER_PASSWORD` varchar(255) NOT NULL,
  PRIMARY KEY (`REPO_ID`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=latin1;


/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
