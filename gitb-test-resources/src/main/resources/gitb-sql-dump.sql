-- MySQL dump 10.13  Distrib 5.5.43, for debian-linux-gnu (x86_64)
--
-- Host: localhost    Database: gitb
-- ------------------------------------------------------
-- Server version	5.5.43-0ubuntu0.14.04.1

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
-- Table structure for table `Actors`
--

DROP TABLE IF EXISTS `Actors`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `Actors` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `actorId` varchar(254) NOT NULL,
  `name` varchar(254) NOT NULL,
  `description` text,
  `domain` bigint(20) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `actors_aid_unq_idx` (`actorId`)
) ENGINE=InnoDB AUTO_INCREMENT=7 DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `Actors`
--

LOCK TABLES `Actors` WRITE;
/*!40000 ALTER TABLE `Actors` DISABLE KEYS */;
INSERT INTO `Actors` VALUES (1,'SenderAccessPoint','Sender Access Point','Sends business messages to a Receiver Access Point using the AS2 protocol.',1),(2,'ReceiverAccessPoint','Receiver Access Point','Receives business messages from Sender Access Point using the AS2 protocol and validates it',1),(3,'ServiceMetadataLocator','Service Metadata Locator','A service which provides a client with the capability of discovering the Service Metadata Publisher endpoint associated with a particular participant identifier.',1),(4,'ServiceMetadataPublisher','Service Metadata Publisher','Provides a service on the network where information about services of specific participant businesses can be found and retrieved.',1),(5,'SMLClient','SML Client','Sends DNS queries associated with a participant identifier to a Service Metadata Locator',1),(6,'SMPClient','SMP Client','Sends HTTP requests to Service Metadata Publisher to retrieve information about participant businesses.',1);
/*!40000 ALTER TABLE `Actors` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `Configurations`
--

DROP TABLE IF EXISTS `Configurations`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `Configurations` (
  `system` bigint(20) NOT NULL,
  `parameter` bigint(20) NOT NULL,
  `endpoint` bigint(20) NOT NULL,
  `value` blob NOT NULL,
  PRIMARY KEY (`system`,`parameter`,`endpoint`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `Configurations`
--

LOCK TABLES `Configurations` WRITE;
/*!40000 ALTER TABLE `Configurations` DISABLE KEYS */;
/*!40000 ALTER TABLE `Configurations` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `Domains`
--

DROP TABLE IF EXISTS `Domains`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `Domains` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `sname` varchar(254) NOT NULL,
  `fname` varchar(254) NOT NULL,
  `description` text,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `Domains`
--

LOCK TABLES `Domains` WRITE;
/*!40000 ALTER TABLE `Domains` DISABLE KEYS */;
INSERT INTO `Domains` VALUES (1,'PEPPOL','Pan-European Public Procurement Online','Many EU countries use electronic procurement (eProcurement) to make bidding for public sector contracts simpler and more efficient. However, these national solutions have limited communication across borders. PEPPOL will make electronic communication between companies and government bodies possible for all procurement processes in the EU. It will connect existing national systems, crucial for allowing businesses to bid for public sector contracts anywhere in the EU; an important step towards achieving the Single European Market.');
/*!40000 ALTER TABLE `Domains` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `EndpointSupportsTransactions`
--

DROP TABLE IF EXISTS `EndpointSupportsTransactions`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `EndpointSupportsTransactions` (
  `actor` bigint(20) NOT NULL,
  `endpoint` varchar(254) NOT NULL,
  `transaction` varchar(254) NOT NULL,
  PRIMARY KEY (`actor`,`endpoint`,`transaction`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `EndpointSupportsTransactions`
--

LOCK TABLES `EndpointSupportsTransactions` WRITE;
/*!40000 ALTER TABLE `EndpointSupportsTransactions` DISABLE KEYS */;
/*!40000 ALTER TABLE `EndpointSupportsTransactions` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `Endpoints`
--

DROP TABLE IF EXISTS `Endpoints`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `Endpoints` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `name` varchar(254) NOT NULL,
  `description` text,
  `actor` bigint(20) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `endp_act_unq_idx` (`name`,`actor`)
) ENGINE=InnoDB AUTO_INCREMENT=7 DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `Endpoints`
--

LOCK TABLES `Endpoints` WRITE;
/*!40000 ALTER TABLE `Endpoints` DISABLE KEYS */;
INSERT INTO `Endpoints` VALUES (1,'as2',NULL,1),(2,'as2',NULL,2),(3,'http',NULL,3),(4,'http',NULL,4),(5,'http',NULL,5),(6,'http',NULL,6);
/*!40000 ALTER TABLE `Endpoints` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `Options`
--

DROP TABLE IF EXISTS `Options`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `Options` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `sname` varchar(254) NOT NULL,
  `fname` varchar(254) NOT NULL,
  `description` text,
  `actor` bigint(20) NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `Options`
--

LOCK TABLES `Options` WRITE;
/*!40000 ALTER TABLE `Options` DISABLE KEYS */;
/*!40000 ALTER TABLE `Options` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `Organizations`
--

DROP TABLE IF EXISTS `Organizations`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `Organizations` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `sname` varchar(254) NOT NULL,
  `fname` varchar(254) NOT NULL,
  `type` smallint(6) NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `Organizations`
--

LOCK TABLES `Organizations` WRITE;
/*!40000 ALTER TABLE `Organizations` DISABLE KEYS */;
INSERT INTO `Organizations` VALUES (1,'GITB','GITB',1),(2,'GITB','GITB',1);
/*!40000 ALTER TABLE `Organizations` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `Parameters`
--

DROP TABLE IF EXISTS `Parameters`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `Parameters` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `name` varchar(254) NOT NULL,
  `description` text,
  `use` varchar(254) NOT NULL,
  `kind` varchar(254) NOT NULL,
  `endpoint` bigint(20) NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=18 DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `Parameters`
--

LOCK TABLES `Parameters` WRITE;
/*!40000 ALTER TABLE `Parameters` DISABLE KEYS */;
INSERT INTO `Parameters` VALUES (1,'network.host',NULL,'R','SIMPLE',1),(2,'network.port',NULL,'R','SIMPLE',1),(3,'public.key',NULL,'R','BINARY',1),(4,'participant.identifier',NULL,'R','SIMPLE',1),(5,'network.host',NULL,'R','SIMPLE',2),(6,'network.port',NULL,'R','SIMPLE',2),(7,'http.uri',NULL,'O','SIMPLE',2),(8,'public.key',NULL,'R','BINARY',2),(9,'participant.identifier',NULL,'R','SIMPLE',2),(10,'network.host',NULL,'R','SIMPLE',3),(11,'network.port',NULL,'R','SIMPLE',3),(12,'network.host',NULL,'R','SIMPLE',4),(13,'network.port',NULL,'R','SIMPLE',4),(14,'network.host',NULL,'R','SIMPLE',5),(15,'network.port',NULL,'R','SIMPLE',5),(16,'network.host',NULL,'R','SIMPLE',6),(17,'network.port',NULL,'R','SIMPLE',6);
/*!40000 ALTER TABLE `Parameters` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `SpecificationHasActors`
--

DROP TABLE IF EXISTS `SpecificationHasActors`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `SpecificationHasActors` (
  `spec_id` bigint(20) NOT NULL,
  `actor_id` bigint(20) NOT NULL,
  PRIMARY KEY (`spec_id`,`actor_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `SpecificationHasActors`
--

LOCK TABLES `SpecificationHasActors` WRITE;
/*!40000 ALTER TABLE `SpecificationHasActors` DISABLE KEYS */;
INSERT INTO `SpecificationHasActors` VALUES (2,1),(2,2),(2,3),(2,4),(3,1),(3,2),(3,3),(3,4),(6,1),(6,2),(6,3),(6,4),(8,3),(8,5),(9,4),(9,6);
/*!40000 ALTER TABLE `SpecificationHasActors` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `Specifications`
--

DROP TABLE IF EXISTS `Specifications`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `Specifications` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `sname` varchar(254) NOT NULL,
  `fname` varchar(254) NOT NULL,
  `urls` varchar(254) DEFAULT NULL,
  `diagram` varchar(254) DEFAULT NULL,
  `description` text,
  `type` smallint(6) NOT NULL,
  `domain` bigint(20) NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=10 DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `Specifications`
--

LOCK TABLES `Specifications` WRITE;
/*!40000 ALTER TABLE `Specifications` DISABLE KEYS */;
INSERT INTO `Specifications` VALUES (1,'PEPPOL BIS 1A','PEPPOL BIS 1A Catalog','https://joinup.ec.europa.eu/svn/peppol/PostAward/CatalogueOnly1A/',NULL,'The purpose of this specification is to describe a common format for the catalogue message in the European market, and to facilitate an efficient implementation and increased use of electronic collaboration regarding the sourcing process based on this format.',2,1),(2,'PEPPOL BIS 3A','PEPPOL BIS 3A Order','https://joinup.ec.europa.eu/svn/peppol/PostAward/OrderOnly3A/',NULL,'The purpose of this specification is to describe a common format for the order message in the European market, and to facilitate an efficient implementation and increased use of electronic collaboration regarding the ordering process based on this format.',2,1),(3,'PEPPOL BIS 4A','PEPPOL BIS 4A Invoice','https://joinup.ec.europa.eu/svn/peppol/PostAward/InvoiceOnly4A',NULL,'The purpose of this specification is to describe a common format for the invoice message in the European market, and to facilitate an efficient implementation and increased use of electronic collaboration regarding the invoicing process based on this format.',2,1),(4,'PEPPOL BIS 5A','PEPPOL BIS 5A Billing','https://joinup.ec.europa.eu/svn/peppol/PostAward/Billing5A/',NULL,'The purpose of this specification is to describe a common format for the invoice and credit note messages in the European market, and to facilitate an efficient implementation and increased use of electronic collaboration regarding the billing process based on these formats.',2,1),(5,'PEPPOL BIS 28A','PEPPOL BIS 28A Ordering','https://joinup.ec.europa.eu/svn/peppol/PostAward/Ordering28A/',NULL,'The purpose of this specification is to describe a common format for the order and order response message in the European market, and to facilitate an efficient implementation and increased use of electronic collaboration regarding the ordering process based on these  formats.',2,1),(6,'PEPPOL BIS 30A','PEPPOL BIS 30A Despatch Advice','https://joinup.ec.europa.eu/svn/peppol/PostAward/DespatchAdvice30A/',NULL,'The purpose of this specification is to describe a common format for the order and order response message in the European market, and to facilitate an efficient implementation and increased use of electronic collaboration regarding the ordering process based on these  formats.',2,1),(7,'PEPPOL BIS 36A','PEPPOL BIS 36A Message Level Response','https://joinup.ec.europa.eu/svn/peppol/PostAward/MessageLevelResponse36A/',NULL,'The purpose of this specification is to describe a common format for the message level response\nmessage in the European market, and to facilitate an efficient implementation and increased use of\nelectronic collaboration regarding the message level response process based on this format.',2,1),(8,'SML','Service Metadata Locator',NULL,NULL,'The Service Metadata Locator service specification is based on the use of DNS (Domain Name System) lookups to find the address of the Service Metadata for a given participant ID. This approach has the advantage that it does not need a single central server to run the\\nDiscovery interface, with its associated single point of failure. Instead the already distributed and highly redundant infrastructure which supports DNS is used. The SML service itself thus plays the role of providing controlled access to the creation and update of entries in the DNS.',2,1),(9,'SMP','Service Metadata Publisher',NULL,NULL,'This specification describes the REST (Representational State Transfer) interface for Service Metadata Publication within the Business Document Exchange Network (BUSDOX). It describes the request/response exchanges between a Service Metadata Publisher and a client wishing to discover endpoint information. A client could be an end-user business application or an Access Point. It also defines the request processing that must happen at the client.',2,1);
/*!40000 ALTER TABLE `Specifications` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `SystemHasAdmins`
--

DROP TABLE IF EXISTS `SystemHasAdmins`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `SystemHasAdmins` (
  `sut_id` bigint(20) NOT NULL,
  `user_id` bigint(20) NOT NULL,
  PRIMARY KEY (`sut_id`,`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `SystemHasAdmins`
--

LOCK TABLES `SystemHasAdmins` WRITE;
/*!40000 ALTER TABLE `SystemHasAdmins` DISABLE KEYS */;
/*!40000 ALTER TABLE `SystemHasAdmins` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `SystemImplementsActors`
--

DROP TABLE IF EXISTS `SystemImplementsActors`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `SystemImplementsActors` (
  `sut_id` bigint(20) NOT NULL,
  `spec_id` bigint(20) NOT NULL,
  `actor_id` bigint(20) NOT NULL,
  PRIMARY KEY (`sut_id`,`spec_id`,`actor_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `SystemImplementsActors`
--

LOCK TABLES `SystemImplementsActors` WRITE;
/*!40000 ALTER TABLE `SystemImplementsActors` DISABLE KEYS */;
INSERT INTO `SystemImplementsActors` VALUES (1,2,1),(1,3,1),(1,6,1),(1,8,5),(1,9,6);
/*!40000 ALTER TABLE `SystemImplementsActors` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `SystemImplementsOptions`
--

DROP TABLE IF EXISTS `SystemImplementsOptions`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `SystemImplementsOptions` (
  `sut_id` bigint(20) NOT NULL,
  `option_id` bigint(20) NOT NULL,
  PRIMARY KEY (`sut_id`,`option_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `SystemImplementsOptions`
--

LOCK TABLES `SystemImplementsOptions` WRITE;
/*!40000 ALTER TABLE `SystemImplementsOptions` DISABLE KEYS */;
/*!40000 ALTER TABLE `SystemImplementsOptions` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `Systems`
--

DROP TABLE IF EXISTS `Systems`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `Systems` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `sname` varchar(254) NOT NULL,
  `fname` varchar(254) NOT NULL,
  `description` text,
  `version` varchar(254) NOT NULL,
  `owner` bigint(20) NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `Systems`
--

LOCK TABLES `Systems` WRITE;
/*!40000 ALTER TABLE `Systems` DISABLE KEYS */;
INSERT INTO `Systems` VALUES (1,'System','System',NULL,'v1.0',2);
/*!40000 ALTER TABLE `Systems` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `TestCaseCoversOptions`
--

DROP TABLE IF EXISTS `TestCaseCoversOptions`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `TestCaseCoversOptions` (
  `testcase` bigint(20) NOT NULL,
  `option` bigint(20) NOT NULL,
  PRIMARY KEY (`testcase`,`option`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `TestCaseCoversOptions`
--

LOCK TABLES `TestCaseCoversOptions` WRITE;
/*!40000 ALTER TABLE `TestCaseCoversOptions` DISABLE KEYS */;
/*!40000 ALTER TABLE `TestCaseCoversOptions` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `TestCaseHasActors`
--

DROP TABLE IF EXISTS `TestCaseHasActors`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `TestCaseHasActors` (
  `testcase` bigint(20) NOT NULL,
  `specification` bigint(20) NOT NULL,
  `actor` bigint(20) NOT NULL,
  PRIMARY KEY (`testcase`,`specification`,`actor`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `TestCaseHasActors`
--

LOCK TABLES `TestCaseHasActors` WRITE;
/*!40000 ALTER TABLE `TestCaseHasActors` DISABLE KEYS */;
INSERT INTO `TestCaseHasActors` VALUES (1,2,1),(1,2,2),(2,2,1),(2,2,2),(3,2,1),(3,2,2),(3,2,3),(3,2,4),(4,2,1),(4,2,2),(4,2,3),(4,2,4),(5,2,1),(5,2,2),(6,2,1),(6,2,2),(7,3,1),(7,3,2),(8,3,1),(8,3,2),(9,3,1),(9,3,2),(9,3,3),(9,3,4),(10,3,1),(10,3,2),(10,3,3),(10,3,4),(11,3,1),(11,3,2),(12,3,1),(12,3,2),(13,6,1),(13,6,2),(14,6,1),(14,6,2),(15,6,1),(15,6,2),(15,6,3),(15,6,4),(16,6,1),(16,6,2),(16,6,3),(16,6,4),(17,6,1),(17,6,2),(18,6,1),(18,6,2),(19,8,5),(20,9,6);
/*!40000 ALTER TABLE `TestCaseHasActors` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `TestCases`
--

DROP TABLE IF EXISTS `TestCases`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `TestCases` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `sname` varchar(254) NOT NULL,
  `fname` varchar(254) NOT NULL,
  `version` varchar(254) NOT NULL,
  `authors` varchar(254) DEFAULT NULL,
  `original_date` varchar(254) DEFAULT NULL,
  `modification_date` varchar(254) DEFAULT NULL,
  `description` text,
  `keywords` varchar(254) DEFAULT NULL,
  `type` smallint(6) NOT NULL,
  `path` varchar(254) NOT NULL,
  `target_spec` bigint(20) NOT NULL,
  `target_actors` varchar(254) DEFAULT NULL,
  `target_options` varchar(254) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `tc_sn_vsn_idx` (`sname`,`version`)
) ENGINE=InnoDB AUTO_INCREMENT=21 DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `TestCases`
--

LOCK TABLES `TestCases` WRITE;
/*!40000 ALTER TABLE `TestCases` DISABLE KEYS */;
INSERT INTO `TestCases` VALUES (1,'PEPPOL-Interoperability-Order','PEPPOL-Interoperability-Order','0.1',NULL,NULL,NULL,'The objective of this Test Scenario is to ensure the Sender Access Point (the System Under\n            Test) can submit a conformant PEPPOL BIS 3A electronic order to a Receiver Access Point using the AS2\n            protocol. Then submitted document is validated by Validex.\n        ',NULL,1,'Peppol_BIS_3A_Order/testcases/PEPPOL-Interoperability-Order.xml',0,'SenderAccessPoint,ReceiverAccessPoint',NULL),(2,'PEPPOL-ReceiverAccessPoint-Order','PEPPOL-ReceiverAccessPoint-Order','0.1',NULL,NULL,NULL,'The objective of this Test Scenario is to ensure the Receiver Access Point (the System Under\n            Test) can receive a conformant PEPPOL BIS 3A electronic order from a Sender Access Point using the AS2\n            protocol.\n        ',NULL,0,'Peppol_BIS_3A_Order/testcases/PEPPOL-ReceiverAccessPoint-Order.xml',0,'SenderAccessPoint,ReceiverAccessPoint',NULL),(3,'PEPPOL-SenderAccessPoint-Order-BusDox','PEPPOL-SenderAccessPoint-Order-BusDox','0.1',NULL,NULL,NULL,'The objective of this Test Scenario is to ensure the Sender Access Point (the System Under\n            Test) is capable of querying both SML and SMP as well as submitting a conformant PEPPOL BIS 3A electronic\n            order to a Receiver Access Point using the AS2 protocol. Then submitted document is validated by Validex.\n        ',NULL,0,'Peppol_BIS_3A_Order/testcases/PEPPOL-SenderAccessPoint-Order-BusDox-Validex.xml',0,'SenderAccessPoint,ReceiverAccessPoint,ServiceMetadataLocator,ServiceMetadataPublisher',NULL),(4,'PEPPOL-SenderAccessPoint-Order-BusDox-Validex','PEPPOL-SenderAccessPoint-Order-BusDox-Validex','0.1',NULL,NULL,NULL,'The objective of this Test Scenario is to ensure the Sender Access Point (the System Under\n            Test) is capable of querying both SML and SMP as well as submitting a conformant PEPPOL BIS 3A electronic\n            order to a Receiver Access Point using the AS2 protocol. Then submitted document is validated by UBL 2.1\n            schema and PEPPOL Schematron rules.\n        ',NULL,0,'Peppol_BIS_3A_Order/testcases/PEPPOL-SenderAccessPoint-Order-BusDox.xml',0,'SenderAccessPoint,ReceiverAccessPoint,ServiceMetadataLocator,ServiceMetadataPublisher',NULL),(5,'PEPPOL-SenderAccessPoint-Order-Validation','PEPPOL-SenderAccessPoint-Order-Validation','0.1',NULL,NULL,NULL,'The objective of this Test Scenario is to ensure the Sender Access Point (the System Under\n            Test) can submit a conformant PEPPOL BIS 3A electronic order to a Receiver Access Point using the AS2\n            protocol. Then submitted document is validated by UBL 2.1 schema and PEPPOL Schematron rules.\n        ',NULL,0,'Peppol_BIS_3A_Order/testcases/PEPPOL-SenderAccessPoint-Order-Validation.xml',0,'SenderAccessPoint,ReceiverAccessPoint',NULL),(6,'PEPPOL-SenderAccessPoint-Order-Validex','PEPPOL-SenderAccessPoint-Order-Validex','0.1',NULL,NULL,NULL,'The objective of this Test Scenario is to ensure the Sender Access Point (the System Under\n            Test) can submit a conformant PEPPOL BIS 3A electronic order to a Receiver Access Point using the AS2\n            protocol. Then submitted document is validated by Validex.\n        ',NULL,0,'Peppol_BIS_3A_Order/testcases/PEPPOL-SenderAccessPoint-Order-Validex.xml',0,'SenderAccessPoint,ReceiverAccessPoint',NULL),(7,'PEPPOL-Interoperability-Invoice','PEPPOL-Interoperability-Invoice','0.1',NULL,NULL,NULL,'The objective of this Test Scenario is to ensure the Sender Access Point (the System Under\n            Test) can submit a conformant PEPPOL BIS 4A electronic invoice to a Receiver Access Point using the AS2\n            protocol. Then submitted document is validated by Validex.\n        ',NULL,1,'Peppol_BIS_4A_Invoice/testcases/PEPPOL-Interoperability-Invoice.xml',0,'SenderAccessPoint,ReceiverAccessPoint',NULL),(8,'PEPPOL-ReceiverAccessPoint-Invoice','PEPPOL-ReceiverAccessPoint-Invoice','0.1',NULL,NULL,NULL,'The objective of this Test Scenario is to ensure the Receiver Access Point (the System Under\n            Test) can receive a conformant PEPPOL BIS 4A electronic invoice from a Sender Access Point using the AS2\n            protocol.\n        ',NULL,0,'Peppol_BIS_4A_Invoice/testcases/PEPPOL-ReceiverAccessPoint-Invoice.xml',0,'SenderAccessPoint,ReceiverAccessPoint',NULL),(9,'PEPPOL-SenderAccessPoint-Invoice-BusDox','PEPPOL-SenderAccessPoint-Invoice-BusDox','0.1',NULL,NULL,NULL,'The objective of this Test Scenario is to ensure the Sender Access Point (the System Under\n            Test) is capable of querying both SML and SMP as well as submitting a conformant PEPPOL BIS 4A electronic\n            invoice to a Receiver Access Point using the AS2 protocol. Then submitted document is validated by UBL 2.1\n            schema and PEPPOL Schematron rules.\n        ',NULL,0,'Peppol_BIS_4A_Invoice/testcases/PEPPOL-SenderAccessPoint-Invoice-BusDox.xml',0,'SenderAccessPoint,ReceiverAccessPoint,ServiceMetadataLocator,ServiceMetadataPublisher',NULL),(10,'PEPPOL-SenderAccessPoint-Invoice-BusDox-Validex','PEPPOL-SenderAccessPoint-Invoice-BusDox-Validex','0.1',NULL,NULL,NULL,'The objective of this Test Scenario is to ensure the Sender Access Point (the System Under\n            Test) is capable of querying both SML and SMP as well as submitting a conformant PEPPOL BIS 4A electronic\n            invoice to a Receiver Access Point using the AS2 protocol. Then submitted document is validated by Validex.\n        ',NULL,0,'Peppol_BIS_4A_Invoice/testcases/PEPPOL-SenderAccessPoint-Invoice-BusDox-Validex.xml',0,'SenderAccessPoint,ReceiverAccessPoint,ServiceMetadataLocator,ServiceMetadataPublisher',NULL),(11,'PEPPOL-SenderAccessPoint-Invoice-Validation','PEPPOL-SenderAccessPoint-Invoice-Validation','0.1',NULL,NULL,NULL,'The objective of this Test Scenario is to ensure the Sender Access Point (the System Under\n            Test) can submit a conformant PEPPOL BIS 4A electronic invoice to a Receiver Access Point using the AS2\n            protocol. Then submitted document is validated by UBL 2.1 schema and PEPPOL Schematron rules.\n        ',NULL,0,'Peppol_BIS_4A_Invoice/testcases/PEPPOL-SenderAccessPoint-Invoice-Validation.xml',0,'SenderAccessPoint,ReceiverAccessPoint',NULL),(12,'PEPPOL-SenderAccessPoint-Invoice-Validex','PEPPOL-SenderAccessPoint-Invoice-Validex','0.1',NULL,NULL,NULL,'The objective of this Test Scenario is to ensure the Sender Access Point (the System Under\n            Test) can submit a conformant PEPPOL BIS 4A electronic invoice to a Receiver Access Point using the AS2\n            protocol. Then submitted document is validated by Validex.\n        ',NULL,0,'Peppol_BIS_4A_Invoice/testcases/PEPPOL-SenderAccessPoint-Invoice-Validex.xml',0,'SenderAccessPoint,ReceiverAccessPoint',NULL),(13,'PEPPOL-Interoperability-DespatchAdvice','PEPPOL-Interoperability-DespatchAdvice','0.1',NULL,NULL,NULL,'The objective of this Test Scenario is to ensure the Sender Access Point (the System Under\n            Test) can submit a conformant PEPPOL BIS 30A electronic despatch advice to a Receiver Access Point using\n            the AS2 protocol. Then submitted document is validated by Validex.\n        ',NULL,1,'Peppol_BIS_30A_DespatchAdvice/testcases/PEPPOL-Interoperability-DespatchAdvice.xml',0,'SenderAccessPoint,ReceiverAccessPoint',NULL),(14,'PEPPOL-ReceiverAccessPoint-DespatchAdvice','PEPPOL-ReceiverAccessPoint-DespatchAdvice','0.1',NULL,NULL,NULL,'The objective of this Test Scenario is to ensure the Receiver Access Point (the System Under\n            Test) can receive a conformant PEPPOL BIS 30A electronic despatch advice from a Sender Access Point using\n            the AS2 protocol.\n        ',NULL,0,'Peppol_BIS_30A_DespatchAdvice/testcases/PEPPOL-ReceiverAccessPoint-DespatchAdvice.xml',0,'SenderAccessPoint,ReceiverAccessPoint',NULL),(15,'PEPPOL-SenderAccessPoint-DespatchAdvice-BusDox','PEPPOL-SenderAccessPoint-DespatchAdvice-BusDox','0.1',NULL,NULL,NULL,'The objective of this Test Scenario is to ensure the Sender Access Point (the System Under\n            Test) is capable of querying both SML and SMP as well as submitting a conformant PEPPOL BIS 4A electronic\n            despatch advice to a Receiver Access Point using the AS2 protocol. Then submitted document is validated by\n            UBL 2.1 schema and PEPPOL Schematron rules.\n        ',NULL,0,'Peppol_BIS_30A_DespatchAdvice/testcases/PEPPOL-SenderAccessPoint-DespatchAdvice-BusDox.xml',0,'SenderAccessPoint,ReceiverAccessPoint,ServiceMetadataLocator,ServiceMetadataPublisher',NULL),(16,'PEPPOL-SenderAccessPoint-DespatchAdvice-BusDox-Validex','PEPPOL-SenderAccessPoint-DespatchAdvice-BusDox-Validex','0.1',NULL,NULL,NULL,'The objective of this Test Scenario is to ensure the Sender Access Point (the System Under\n            Test) is capable of querying both SML and SMP as well as submitting a conformant PEPPOL BIS 4A electronic\n            despatch advice to a Receiver Access Point using the AS2 protocol. Then submitted document is validated by\n            UBL 2.1 schema and PEPPOL Schematron rules.\n        ',NULL,0,'Peppol_BIS_30A_DespatchAdvice/testcases/PEPPOL-SenderAccessPoint-DespatchAdvice-BusDox-Validex.xml',0,'SenderAccessPoint,ReceiverAccessPoint,ServiceMetadataLocator,ServiceMetadataPublisher',NULL),(17,'PEPPOL-SenderAccessPoint-DespatchAdvice-Validation','PEPPOL-SenderAccessPoint-DespatchAdvice-Validation','0.1',NULL,NULL,NULL,'The objective of this Test Scenario is to ensure the Sender Access Point (the System Under\n            Test) can submit a conformant PEPPOL BIS 4A electronic despatch advice to a Receiver Access Point using the\n            AS2 protocol. Then submitted document is validated by UBL 2.1 schema and PEPPOL Schematron rules.\n        ',NULL,0,'Peppol_BIS_30A_DespatchAdvice/testcases/PEPPOL-SenderAccessPoint-DespatchAdvice-Validation.xml',0,'SenderAccessPoint,ReceiverAccessPoint',NULL),(18,'PEPPOL-SenderAccessPoint-DespatchAdvice-Validex','PEPPOL-SenderAccessPoint-DespatchAdvice-Validex','0.1',NULL,NULL,NULL,'The objective of this Test Scenario is to ensure the Sender Access Point (the System Under\n            Test) can submit a conformant PEPPOL BIS 4A electronic despatch advice to a Receiver Access Point using the\n            AS2 protocol. Then submitted document is validated by Validex.\n        ',NULL,0,'Peppol_BIS_30A_DespatchAdvice/testcases/PEPPOL-SenderAccessPoint-DespatchAdvice-Validex.xml',0,'SenderAccessPoint,ReceiverAccessPoint',NULL),(19,'SMLClient','SML Client','0.1',NULL,NULL,NULL,'This test scenario implements the lookup interface which enables senders to discover\n            service metadata about specific target participants\n        ',NULL,0,'ServiceMetadataLocator/testcases/SMLClient.xml',0,'ServiceMetadataLocator',NULL),(20,'SMPClient','SMP Client','0.1',NULL,NULL,NULL,'This test scenario implements the lookup interface which enables senders to discover\n            service metadata about specific target participants\n        ',NULL,0,'ServiceMetadataPublisher/testcases/SMPClient.xml',0,'ServiceMetadataPublisher',NULL);
/*!40000 ALTER TABLE `TestCases` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `TestResults`
--

DROP TABLE IF EXISTS `TestResults`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `TestResults` (
  `test_session_id` varchar(254) NOT NULL,
  `sut_id` bigint(20) NOT NULL,
  `actor_id` bigint(20) NOT NULL,
  `testcase_id` bigint(20) NOT NULL,
  `result` varchar(254) NOT NULL,
  `start_time` varchar(254) NOT NULL,
  `end_time` varchar(254) DEFAULT NULL,
  `sut_version` varchar(254) DEFAULT NULL,
  `tpl` blob NOT NULL,
  PRIMARY KEY (`test_session_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `TestResults`
--

LOCK TABLES `TestResults` WRITE;
/*!40000 ALTER TABLE `TestResults` DISABLE KEYS */;
/*!40000 ALTER TABLE `TestResults` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `TestStepReports`
--

DROP TABLE IF EXISTS `TestStepReports`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `TestStepReports` (
  `test_session_id` varchar(254) NOT NULL,
  `test_step_id` varchar(254) NOT NULL,
  `result` smallint(6) NOT NULL,
  `report_path` varchar(254) NOT NULL,
  PRIMARY KEY (`test_session_id`,`test_step_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `TestStepReports`
--

LOCK TABLES `TestStepReports` WRITE;
/*!40000 ALTER TABLE `TestStepReports` DISABLE KEYS */;
/*!40000 ALTER TABLE `TestStepReports` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `TestSuiteHasActors`
--

DROP TABLE IF EXISTS `TestSuiteHasActors`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `TestSuiteHasActors` (
  `testsuite` bigint(20) NOT NULL,
  `actor` bigint(20) NOT NULL,
  PRIMARY KEY (`testsuite`,`actor`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `TestSuiteHasActors`
--

LOCK TABLES `TestSuiteHasActors` WRITE;
/*!40000 ALTER TABLE `TestSuiteHasActors` DISABLE KEYS */;
INSERT INTO `TestSuiteHasActors` VALUES (1,1),(1,2),(1,3),(1,4),(2,1),(2,2),(2,3),(2,4),(3,1),(3,2),(3,3),(3,4),(4,3),(4,5),(5,4),(5,6);
/*!40000 ALTER TABLE `TestSuiteHasActors` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `TestSuiteHasTestCases`
--

DROP TABLE IF EXISTS `TestSuiteHasTestCases`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `TestSuiteHasTestCases` (
  `testsuite` bigint(20) NOT NULL,
  `testcase` bigint(20) NOT NULL,
  PRIMARY KEY (`testsuite`,`testcase`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `TestSuiteHasTestCases`
--

LOCK TABLES `TestSuiteHasTestCases` WRITE;
/*!40000 ALTER TABLE `TestSuiteHasTestCases` DISABLE KEYS */;
INSERT INTO `TestSuiteHasTestCases` VALUES (1,1),(1,2),(1,3),(1,4),(1,5),(1,6),(2,7),(2,8),(2,9),(2,10),(2,11),(2,12),(3,13),(3,14),(3,15),(3,16),(3,17),(3,18),(4,19),(5,20);
/*!40000 ALTER TABLE `TestSuiteHasTestCases` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `TestSuites`
--

DROP TABLE IF EXISTS `TestSuites`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `TestSuites` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `sname` varchar(254) NOT NULL,
  `fname` varchar(254) NOT NULL,
  `version` varchar(254) NOT NULL,
  `authors` varchar(254) DEFAULT NULL,
  `original_date` varchar(254) DEFAULT NULL,
  `modification_date` varchar(254) DEFAULT NULL,
  `description` text,
  `keywords` varchar(254) DEFAULT NULL,
  `specification` bigint(20) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `ts_sn_vsn_idx` (`sname`,`version`)
) ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `TestSuites`
--

LOCK TABLES `TestSuites` WRITE;
/*!40000 ALTER TABLE `TestSuites` DISABLE KEYS */;
INSERT INTO `TestSuites` VALUES (1,'Peppol_BIS_3A_Order','Peppol_BIS_3A_Order','0.1',NULL,NULL,NULL,NULL,NULL,2),(2,'Peppol_BIS_4A_Invoice','Peppol_BIS_4A_Invoice','0.1',NULL,NULL,NULL,NULL,NULL,3),(3,'Peppol_BIS_30A_DespatchAdvice','Peppol_BIS_30A_DespatchAdvice','0.1',NULL,NULL,NULL,NULL,NULL,6),(4,'ServiceMetadataLocator','ServiceMetadataLocator','0.1',NULL,NULL,NULL,NULL,NULL,8),(5,'ServiceMetadataPublisher','ServiceMetadataPublisher','0.1',NULL,NULL,NULL,NULL,NULL,9);
/*!40000 ALTER TABLE `TestSuites` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `Transactions`
--

DROP TABLE IF EXISTS `Transactions`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `Transactions` (
  `sname` varchar(254) NOT NULL,
  `fname` varchar(254) NOT NULL,
  `description` text,
  `domain` bigint(20) NOT NULL,
  PRIMARY KEY (`sname`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `Transactions`
--

LOCK TABLES `Transactions` WRITE;
/*!40000 ALTER TABLE `Transactions` DISABLE KEYS */;
/*!40000 ALTER TABLE `Transactions` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `Users`
--

DROP TABLE IF EXISTS `Users`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `Users` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `name` varchar(254) NOT NULL,
  `email` varchar(254) NOT NULL,
  `password` varchar(254) NOT NULL,
  `role` smallint(6) NOT NULL,
  `organization` bigint(20) NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `Users`
--

LOCK TABLES `Users` WRITE;
/*!40000 ALTER TABLE `Users` DISABLE KEYS */;
INSERT INTO `Users` VALUES (1,'admin','admin@gitb.com','admin',4,1),(2,'user','user@gitb.com','gitb',1,2);
/*!40000 ALTER TABLE `Users` ENABLE KEYS */;
UNLOCK TABLES;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2015-06-10 10:57:47
