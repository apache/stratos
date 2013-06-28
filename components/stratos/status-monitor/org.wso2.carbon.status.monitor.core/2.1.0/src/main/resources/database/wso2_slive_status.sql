-- MySQL dump 10.13  Distrib 5.1.41, for debian-linux-gnu (x86_64)
--
-- Host: localhost    Database: wso2_slive_status
-- ------------------------------------------------------
-- Server version	5.1.41-3ubuntu12.10

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Table structure for table `WSL_MAIL_CONTENT_HASH`
--
drop database stratos_status;
create database stratos_status;
use stratos_status;
DROP TABLE IF EXISTS `WSL_MAIL_CONTENT_HASH`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `WSL_MAIL_CONTENT_HASH` (
  `WSL_HASH_ID` int(11) NOT NULL AUTO_INCREMENT,
  `WSL_MAIL_HASH` varchar(255) NOT NULL,
  `WSL_IS_MAIL_SENT` tinyint(1) NOT NULL DEFAULT '0',
  PRIMARY KEY (`WSL_HASH_ID`)
) ENGINE=MyISAM AUTO_INCREMENT=88 DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `WSL_SERVICE`
--

DROP TABLE IF EXISTS `WSL_SERVICE`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `WSL_SERVICE` (
  `WSL_ID` int(11) NOT NULL AUTO_INCREMENT,
  `WSL_NAME` varchar(265) NOT NULL,
  PRIMARY KEY (`WSL_ID`)
) ENGINE=MyISAM AUTO_INCREMENT=14 DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `WSL_SERVICE_HEARTBEAT`
--

DROP TABLE IF EXISTS `WSL_SERVICE_HEARTBEAT`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `WSL_SERVICE_HEARTBEAT` (
  `WSL_ID` bigint(20) NOT NULL AUTO_INCREMENT,
  `WSL_SERVICE_ID` int(11) NOT NULL,
  `WSL_STATUS` tinyint(1) NOT NULL DEFAULT '0',
  `WSL_TIMESTAMP` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`WSL_ID`)
) ENGINE=MyISAM AUTO_INCREMENT=50286 DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `WSL_SERVICE_STATE`
--

DROP TABLE IF EXISTS `WSL_SERVICE_STATE`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `WSL_SERVICE_STATE` (
  `WSL_ID` bigint(20) NOT NULL AUTO_INCREMENT,
  `WSL_SERVICE_ID` int(11) NOT NULL,
  `WSL_STATE_ID` int(11) NOT NULL,
  `WSL_TIMESTAMP` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`WSL_ID`)
) ENGINE=MyISAM AUTO_INCREMENT=45884 DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `WSL_SERVICE_STATE_DETAIL`
--

DROP TABLE IF EXISTS `WSL_SERVICE_STATE_DETAIL`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `WSL_SERVICE_STATE_DETAIL` (
  `WSL_ID` bigint(20) NOT NULL AUTO_INCREMENT,
  `WSL_SERVICE_STATE_ID` bigint(20) NOT NULL,
  `WSL_DETAIL` varchar(2048) NOT NULL,
  `WSL_TIMESTAMP` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`WSL_ID`)
) ENGINE=MyISAM AUTO_INCREMENT=1249 DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `WSL_STATE`
--

DROP TABLE IF EXISTS `WSL_STATE`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `WSL_STATE` (
  `WSL_ID` int(11) NOT NULL AUTO_INCREMENT,
  `WSL_NAME` varchar(256) NOT NULL,
  PRIMARY KEY (`WSL_ID`)
) ENGINE=MyISAM AUTO_INCREMENT=5 DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `WSL_USER`
--

DROP TABLE IF EXISTS `WSL_USER`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `WSL_USER` (
  `WSL_UID` int(11) NOT NULL AUTO_INCREMENT,
  `WSL_MAIL` varchar(255) NOT NULL,
  PRIMARY KEY (`WSL_UID`),
  UNIQUE KEY `WSL_UID` (`WSL_UID`)
) ENGINE=MyISAM AUTO_INCREMENT=6 DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

LOCK TABLES `WSL_SERVICE` WRITE;
/*!40000 ALTER TABLE `WSL_SERVICE` DISABLE KEYS */;
INSERT INTO `WSL_SERVICE` VALUES (1,'StratosLive Manager'),(2,'StratosLive Enterprise Service Bus'),(3,'StratosLive Application Server'),(4,'StratosLive Data Services Server'),(5,'StratosLive Governance Registry'),(6,'StratosLive Identity Server'),(7,'StratosLive Business Activity Monitor'),(8,'StratosLive Business Process Server'),(9,'StratosLive Business Rules Server'),(10,'StratosLive Mashup Server'),(11,'StratosLive Gadget Server'),(12,'StratosLive Complex Event Processing Server'),(13,'StratosLive Message Broker');
/*!40000 ALTER TABLE `WSL_SERVICE` ENABLE KEYS */;
UNLOCK TABLES;

LOCK TABLES `WSL_STATE` WRITE;
/*!40000 ALTER TABLE `WSL_STATE` DISABLE KEYS */;
INSERT INTO `WSL_STATE` VALUES (1,'Up & Running'),(2,'Broken'),(3,'Down'),(4,'Resolved');
/*!40000 ALTER TABLE `WSL_STATE` ENABLE KEYS */;
UNLOCK TABLES;


/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2011-09-06  9:39:52
