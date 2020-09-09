SET sql_mode='NO_AUTO_VALUE_ON_ZERO';

use gitb;

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
-- Table structure for table `actors`
--

DROP TABLE IF EXISTS `actors`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `actors` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `actorId` varchar(254) NOT NULL,
  `name` varchar(254) NOT NULL,
  `description` text,
  `domain` bigint(20) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `actors_aid_unq_idx` (`actorId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `configurations`
--

DROP TABLE IF EXISTS `configurations`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `configurations` (
  `system` bigint(20) NOT NULL,
  `parameter` bigint(20) NOT NULL,
  `endpoint` bigint(20) NOT NULL,
  `value` blob NOT NULL,
  PRIMARY KEY (`system`,`parameter`,`endpoint`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `domains`
--

DROP TABLE IF EXISTS `domains`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `domains` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `sname` varchar(254) NOT NULL,
  `fname` varchar(254) NOT NULL,
  `description` text,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `endpoints`
--

DROP TABLE IF EXISTS `endpoints`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `endpoints` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `name` varchar(254) NOT NULL,
  `description` text,
  `actor` bigint(20) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `endp_act_unq_idx` (`name`,`actor`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `endpointsupportstransactions`
--

DROP TABLE IF EXISTS `endpointsupportstransactions`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `endpointsupportstransactions` (
  `actor` bigint(20) NOT NULL,
  `endpoint` varchar(254) NOT NULL,
  `transaction` varchar(254) NOT NULL,
  PRIMARY KEY (`actor`,`endpoint`,`transaction`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `options`
--

DROP TABLE IF EXISTS `options`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `options` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `sname` varchar(254) NOT NULL,
  `fname` varchar(254) NOT NULL,
  `description` text,
  `actor` bigint(20) NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `organizations`
--

DROP TABLE IF EXISTS `organizations`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `organizations` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `sname` varchar(254) NOT NULL,
  `fname` varchar(254) NOT NULL,
  `type` smallint(6) NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `parameters`
--

DROP TABLE IF EXISTS `parameters`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `parameters` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `name` varchar(254) NOT NULL,
  `description` text,
  `use` varchar(254) NOT NULL,
  `kind` varchar(254) NOT NULL,
  `endpoint` bigint(20) NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `specificationhasactors`
--

DROP TABLE IF EXISTS `specificationhasactors`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `specificationhasactors` (
  `spec_id` bigint(20) NOT NULL,
  `actor_id` bigint(20) NOT NULL,
  PRIMARY KEY (`spec_id`,`actor_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `specifications`
--

DROP TABLE IF EXISTS `specifications`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `specifications` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `sname` varchar(254) NOT NULL,
  `fname` varchar(254) NOT NULL,
  `urls` varchar(254) DEFAULT NULL,
  `diagram` varchar(254) DEFAULT NULL,
  `description` text,
  `type` smallint(6) NOT NULL,
  `domain` bigint(20) NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `systemhasadmins`
--

DROP TABLE IF EXISTS `systemhasadmins`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `systemhasadmins` (
  `sut_id` bigint(20) NOT NULL,
  `user_id` bigint(20) NOT NULL,
  PRIMARY KEY (`sut_id`,`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `systemimplementsactors`
--

DROP TABLE IF EXISTS `systemimplementsactors`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `systemimplementsactors` (
  `sut_id` bigint(20) NOT NULL,
  `spec_id` bigint(20) NOT NULL,
  `actor_id` bigint(20) NOT NULL,
  PRIMARY KEY (`sut_id`,`spec_id`,`actor_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `systemimplementsoptions`
--

DROP TABLE IF EXISTS `systemimplementsoptions`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `systemimplementsoptions` (
  `sut_id` bigint(20) NOT NULL,
  `option_id` bigint(20) NOT NULL,
  PRIMARY KEY (`sut_id`,`option_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `systems`
--

DROP TABLE IF EXISTS `systems`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `systems` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `sname` varchar(254) NOT NULL,
  `fname` varchar(254) NOT NULL,
  `description` text,
  `version` varchar(254) NOT NULL,
  `owner` bigint(20) NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `testcasecoversoptions`
--

DROP TABLE IF EXISTS `testcasecoversoptions`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `testcasecoversoptions` (
  `testcase` bigint(20) NOT NULL,
  `option` bigint(20) NOT NULL,
  PRIMARY KEY (`testcase`,`option`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `testcasehasactors`
--

DROP TABLE IF EXISTS `testcasehasactors`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `testcasehasactors` (
  `testcase` bigint(20) NOT NULL,
  `specification` bigint(20) NOT NULL,
  `actor` bigint(20) NOT NULL,
  PRIMARY KEY (`testcase`,`specification`,`actor`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `testcases`
--

DROP TABLE IF EXISTS `testcases`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `testcases` (
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `testresults`
--

DROP TABLE IF EXISTS `testresults`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `testresults` (
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
-- Table structure for table `teststepreports`
--

DROP TABLE IF EXISTS `teststepreports`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `teststepreports` (
  `test_session_id` varchar(254) NOT NULL,
  `test_step_id` varchar(254) NOT NULL,
  `result` smallint(6) NOT NULL,
  `report_path` varchar(254) NOT NULL,
  PRIMARY KEY (`test_session_id`,`test_step_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `testsuitehasactors`
--

DROP TABLE IF EXISTS `testsuitehasactors`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `testsuitehasactors` (
  `testsuite` bigint(20) NOT NULL,
  `actor` bigint(20) NOT NULL,
  PRIMARY KEY (`testsuite`,`actor`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `testsuitehastestcases`
--

DROP TABLE IF EXISTS `testsuitehastestcases`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `testsuitehastestcases` (
  `testsuite` bigint(20) NOT NULL,
  `testcase` bigint(20) NOT NULL,
  PRIMARY KEY (`testsuite`,`testcase`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `testsuites`
--

DROP TABLE IF EXISTS `testsuites`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `testsuites` (
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `transactions`
--

DROP TABLE IF EXISTS `transactions`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `transactions` (
  `sname` varchar(254) NOT NULL,
  `fname` varchar(254) NOT NULL,
  `description` text,
  `domain` bigint(20) NOT NULL,
  PRIMARY KEY (`sname`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `users`
--

DROP TABLE IF EXISTS `users`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `users` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `name` varchar(254) NOT NULL,
  `email` varchar(254) NOT NULL,
  `password` varchar(254) NOT NULL,
  `role` smallint(6) NOT NULL,
  `organization` bigint(20) NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

insert into users(name, email, password, role, organization) values('Test User', 'test@test.com', '$2a$04$FqO8.xUGvC7yF7P2mHS9XeypSBuHqs20ob6F6junA5NKEXbVSZwS6', 4, 0);
insert into users(name, email, password, role, organization) values('Test User (Vendor admin)', 'admin@test.com', '$2a$04$FqO8.xUGvC7yF7P2mHS9XeypSBuHqs20ob6F6junA5NKEXbVSZwS6', 1, 1);
insert into organizations(id, sname, fname, type) values(1, 'Test Organization', 'Test Organization', 1);
insert into organizations(id, sname, fname, type) values(0, 'Admin organisation', 'Admin organisation', 1);
insert into systems(sname, fname, description, version, owner) values('Test system', 'Test system', 'System defined for testing purposes.', '0.1', 1);

CREATE TABLE landingpages (
  id BIGINT PRIMARY KEY NOT NULL AUTO_INCREMENT,
  name VARCHAR(255) NOT NULL,
  content LONGTEXT,
  default_flag TINYINT DEFAULT 0 NOT NULL,
  description LONGTEXT
);

CREATE UNIQUE INDEX unique_name ON landingpages (name);
ALTER TABLE organizations ADD landing_page BIGINT NULL;

CREATE TABLE systemconfigurations (
  name VARCHAR(255) PRIMARY KEY NOT NULL,
  parameter VARCHAR(255),
  description VARCHAR(255)
);
INSERT INTO systemconfigurations (name, parameter, description) VALUES ('session_alive_time', '3600', 'session_alive_time');

CREATE TABLE legalnotices(
  id BIGINT PRIMARY KEY NOT NULL AUTO_INCREMENT,
  name VARCHAR(255) NOT NULL,
  content LONGTEXT,
  default_flag TINYINT DEFAULT 0 NOT NULL,
  description LONGTEXT
);
ALTER TABLE legalnotices ADD CONSTRAINT unique_name UNIQUE (name);
 
ALTER TABLE organizations ADD legal_notice BIGINT NULL;

-- Changes for v1.1.0 - START
CREATE TABLE communities (
  id BIGINT(20) NOT NULL AUTO_INCREMENT,
  sname VARCHAR(254) NOT NULL,
  fname VARCHAR(254) NOT NULL,
  domain BIGINT(20) NULL DEFAULT NULL,
  PRIMARY KEY (id));
ALTER TABLE organizations ADD COLUMN community BIGINT(20) NOT NULL AFTER legal_notice;
ALTER TABLE testresults CHANGE COLUMN start_time start_time TIMESTAMP NOT NULL DEFAULT '1970-01-01 00:00:01', CHANGE COLUMN end_time end_time TIMESTAMP NULL DEFAULT NULL;
insert into communities(id, sname, fname) values(0, 'Default community', 'Default community');
update communities set id = 0;
ALTER TABLE organizations ADD COLUMN admin_organization TINYINT(4) NOT NULL DEFAULT '0' AFTER type;
UPDATE organizations SET admin_organization = '1' WHERE id = '0';
ALTER TABLE landingpages ADD COLUMN community BIGINT(20) NOT NULL AFTER description;
ALTER TABLE legalnotices ADD COLUMN community BIGINT(20) NOT NULL AFTER description;
ALTER TABLE legalnotices DROP INDEX unique_name;
ALTER TABLE landingpages DROP INDEX unique_name;
-- Changes for v1.1.0 - END

commit;