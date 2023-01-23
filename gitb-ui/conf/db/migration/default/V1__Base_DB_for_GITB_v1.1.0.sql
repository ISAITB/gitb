SET sql_mode='NO_AUTO_VALUE_ON_ZERO';

use gitb;

--
-- Table structure for table `actors`
--

CREATE TABLE `actors` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `actorId` varchar(254) NOT NULL,
  `name` varchar(254) NOT NULL,
  `description` text,
  `domain` BIGINT NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `actors_aid_unq_idx` (`actorId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

--
-- Table structure for table `configurations`
--

CREATE TABLE `configurations` (
  `system` BIGINT NOT NULL,
  `parameter` BIGINT NOT NULL,
  `endpoint` BIGINT NOT NULL,
  `value` blob NOT NULL,
  PRIMARY KEY (`system`,`parameter`,`endpoint`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

--
-- Table structure for table `domains`
--

CREATE TABLE `domains` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `sname` varchar(254) NOT NULL,
  `fname` varchar(254) NOT NULL,
  `description` text,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

--
-- Table structure for table `endpoints`
--

CREATE TABLE `endpoints` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `name` varchar(254) NOT NULL,
  `description` text,
  `actor` BIGINT NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `endp_act_unq_idx` (`name`,`actor`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

--
-- Table structure for table `endpointsupportstransactions`
--

CREATE TABLE `endpointsupportstransactions` (
  `actor` BIGINT NOT NULL,
  `endpoint` varchar(254) NOT NULL,
  `transaction` varchar(254) NOT NULL,
  PRIMARY KEY (`actor`,`endpoint`,`transaction`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

--
-- Table structure for table `options`
--

CREATE TABLE `options` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `sname` varchar(254) NOT NULL,
  `fname` varchar(254) NOT NULL,
  `description` text,
  `actor` BIGINT NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

--
-- Table structure for table `organizations`
--

CREATE TABLE `organizations` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `sname` varchar(254) NOT NULL,
  `fname` varchar(254) NOT NULL,
  `type` SMALLINT NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

--
-- Table structure for table `parameters`
--

CREATE TABLE `parameters` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `name` varchar(254) NOT NULL,
  `description` text,
  `use` varchar(254) NOT NULL,
  `kind` varchar(254) NOT NULL,
  `endpoint` BIGINT NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

--
-- Table structure for table `specificationhasactors`
--

CREATE TABLE `specificationhasactors` (
  `spec_id` BIGINT NOT NULL,
  `actor_id` BIGINT NOT NULL,
  PRIMARY KEY (`spec_id`,`actor_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

--
-- Table structure for table `specifications`
--

CREATE TABLE `specifications` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `sname` varchar(254) NOT NULL,
  `fname` varchar(254) NOT NULL,
  `urls` varchar(254) DEFAULT NULL,
  `diagram` varchar(254) DEFAULT NULL,
  `description` text,
  `type` SMALLINT NOT NULL,
  `domain` BIGINT NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

--
-- Table structure for table `systemhasadmins`
--

CREATE TABLE `systemhasadmins` (
  `sut_id` BIGINT NOT NULL,
  `user_id` BIGINT NOT NULL,
  PRIMARY KEY (`sut_id`,`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

--
-- Table structure for table `systemimplementsactors`
--

CREATE TABLE `systemimplementsactors` (
  `sut_id` BIGINT NOT NULL,
  `spec_id` BIGINT NOT NULL,
  `actor_id` BIGINT NOT NULL,
  PRIMARY KEY (`sut_id`,`spec_id`,`actor_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

--
-- Table structure for table `systemimplementsoptions`
--

CREATE TABLE `systemimplementsoptions` (
  `sut_id` BIGINT NOT NULL,
  `option_id` BIGINT NOT NULL,
  PRIMARY KEY (`sut_id`,`option_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

--
-- Table structure for table `systems`
--

CREATE TABLE `systems` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `sname` varchar(254) NOT NULL,
  `fname` varchar(254) NOT NULL,
  `description` text,
  `version` varchar(254) NOT NULL,
  `owner` BIGINT NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

--
-- Table structure for table `testcasecoversoptions`
--

CREATE TABLE `testcasecoversoptions` (
  `testcase` BIGINT NOT NULL,
  `option` BIGINT NOT NULL,
  PRIMARY KEY (`testcase`,`option`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

--
-- Table structure for table `testcasehasactors`
--

CREATE TABLE `testcasehasactors` (
  `testcase` BIGINT NOT NULL,
  `specification` BIGINT NOT NULL,
  `actor` BIGINT NOT NULL,
  PRIMARY KEY (`testcase`,`specification`,`actor`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

--
-- Table structure for table `testcases`
--

CREATE TABLE `testcases` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `sname` varchar(254) NOT NULL,
  `fname` varchar(254) NOT NULL,
  `version` varchar(254) NOT NULL,
  `authors` varchar(254) DEFAULT NULL,
  `original_date` varchar(254) DEFAULT NULL,
  `modification_date` varchar(254) DEFAULT NULL,
  `description` text,
  `keywords` varchar(254) DEFAULT NULL,
  `type` SMALLINT NOT NULL,
  `path` varchar(254) NOT NULL,
  `target_spec` BIGINT NOT NULL,
  `target_actors` varchar(254) DEFAULT NULL,
  `target_options` varchar(254) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `tc_sn_vsn_idx` (`sname`,`version`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

--
-- Table structure for table `testresults`
--

CREATE TABLE `testresults` (
  `test_session_id` varchar(254) NOT NULL,
  `sut_id` BIGINT NOT NULL,
  `actor_id` BIGINT NOT NULL,
  `testcase_id` BIGINT NOT NULL,
  `result` varchar(254) NOT NULL,
  `start_time` varchar(254) NOT NULL,
  `end_time` varchar(254) DEFAULT NULL,
  `sut_version` varchar(254) DEFAULT NULL,
  `tpl` blob NOT NULL,
  PRIMARY KEY (`test_session_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

--
-- Table structure for table `teststepreports`
--

CREATE TABLE `teststepreports` (
  `test_session_id` varchar(254) NOT NULL,
  `test_step_id` varchar(254) NOT NULL,
  `result` SMALLINT NOT NULL,
  `report_path` varchar(254) NOT NULL,
  PRIMARY KEY (`test_session_id`,`test_step_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

--
-- Table structure for table `testsuitehasactors`
--

CREATE TABLE `testsuitehasactors` (
  `testsuite` BIGINT NOT NULL,
  `actor` BIGINT NOT NULL,
  PRIMARY KEY (`testsuite`,`actor`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

--
-- Table structure for table `testsuitehastestcases`
--

CREATE TABLE `testsuitehastestcases` (
  `testsuite` BIGINT NOT NULL,
  `testcase` BIGINT NOT NULL,
  PRIMARY KEY (`testsuite`,`testcase`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

--
-- Table structure for table `testsuites`
--

CREATE TABLE `testsuites` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `sname` varchar(254) NOT NULL,
  `fname` varchar(254) NOT NULL,
  `version` varchar(254) NOT NULL,
  `authors` varchar(254) DEFAULT NULL,
  `original_date` varchar(254) DEFAULT NULL,
  `modification_date` varchar(254) DEFAULT NULL,
  `description` text,
  `keywords` varchar(254) DEFAULT NULL,
  `specification` BIGINT NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `ts_sn_vsn_idx` (`sname`,`version`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

--
-- Table structure for table `transactions`
--

CREATE TABLE `transactions` (
  `sname` varchar(254) NOT NULL,
  `fname` varchar(254) NOT NULL,
  `description` text,
  `domain` BIGINT NOT NULL,
  PRIMARY KEY (`sname`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

--
-- Table structure for table `users`
--

CREATE TABLE `users` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `name` varchar(254) NOT NULL,
  `email` varchar(254) NOT NULL,
  `password` varchar(254) NOT NULL,
  `role` SMALLINT NOT NULL,
  `organization` BIGINT NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

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
  id BIGINT NOT NULL AUTO_INCREMENT,
  sname VARCHAR(254) NOT NULL,
  fname VARCHAR(254) NOT NULL,
  domain BIGINT NULL DEFAULT NULL,
  PRIMARY KEY (id));
ALTER TABLE organizations ADD COLUMN community BIGINT NOT NULL AFTER legal_notice;
ALTER TABLE testresults CHANGE COLUMN start_time start_time TIMESTAMP NOT NULL DEFAULT '1970-01-01 00:00:01', CHANGE COLUMN end_time end_time TIMESTAMP NULL DEFAULT NULL;
insert into communities(id, sname, fname) values(0, 'Default community', 'Default community');
update communities set id = 0;
ALTER TABLE organizations ADD COLUMN admin_organization TINYINT NOT NULL DEFAULT '0' AFTER type;
UPDATE organizations SET admin_organization = '1' WHERE id = '0';
ALTER TABLE landingpages ADD COLUMN community BIGINT NOT NULL AFTER description;
ALTER TABLE legalnotices ADD COLUMN community BIGINT NOT NULL AFTER description;
ALTER TABLE legalnotices DROP INDEX unique_name;
ALTER TABLE landingpages DROP INDEX unique_name;
-- Changes for v1.1.0 - END

commit;