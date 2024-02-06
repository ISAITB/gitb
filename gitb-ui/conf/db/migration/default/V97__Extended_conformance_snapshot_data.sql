--
-- CREATE TABLES.
--
-- Test cases.
CREATE TABLE `conformancesnapshottestcases` (
  `id` bigint NOT NULL,
  `sname` varchar(254) NOT NULL,
  `fname` varchar(254) NOT NULL,
  `description` text,
  `identifier` varchar(254) NOT NULL,
  `testsuite_order` smallint NOT NULL,
  `is_optional` smallint NOT NULL DEFAULT '0',
  `is_disabled` smallint NOT NULL DEFAULT '0',
  `tags` text,
  `spec_reference` varchar(254),
  `spec_description` text,
  `spec_link` varchar(254),
  `snapshot_id` BIGINT NOT NULL,
  PRIMARY KEY (`id`,`snapshot_id`),
  KEY `cs_tc_id` (`id`),
  CONSTRAINT `cs_tc_fk_cs` FOREIGN KEY (`snapshot_id`) REFERENCES `conformancesnapshots`(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin;
-- Test suites.
CREATE TABLE `conformancesnapshottestsuites` (
  `id` bigint NOT NULL,
  `sname` varchar(254) NOT NULL,
  `fname` varchar(254) NOT NULL,
  `description` text,
  `identifier` varchar(254) NOT NULL,
  `spec_reference` varchar(254),
  `spec_description` text,
  `spec_link` varchar(254),
  `snapshot_id` BIGINT NOT NULL,
  PRIMARY KEY (`id`,`snapshot_id`),
  KEY `cs_ts_id` (`id`),
  CONSTRAINT `cs_ts_fk_cs` FOREIGN KEY (`snapshot_id`) REFERENCES `conformancesnapshots`(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin;
-- Actors.
CREATE TABLE `conformancesnapshotactors` (
  `id` bigint NOT NULL,
  `actorId` varchar(254) NOT NULL,
  `name` varchar(254) NOT NULL,
  `description` text,
  `api_key` varchar(255) NOT NULL,
  `snapshot_id` BIGINT NOT NULL,
  PRIMARY KEY (`id`,`snapshot_id`),
  KEY `cs_ac_id` (`id`),
  CONSTRAINT `cs_ac_fk_cs` FOREIGN KEY (`snapshot_id`) REFERENCES `conformancesnapshots`(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin;
-- Specifications.
CREATE TABLE `conformancesnapshotspecifications` (
  `id` bigint NOT NULL,
  `sname` varchar(254) NOT NULL,
  `fname` varchar(254) NOT NULL,
  `description` text,
  `display_order` smallint NOT NULL DEFAULT '0',
  `api_key` varchar(255) NOT NULL,
  `snapshot_id` BIGINT NOT NULL,
  PRIMARY KEY (`id`,`snapshot_id`),
  KEY `cs_sp_id` (`id`),
  CONSTRAINT `cs_sp_fk_cs` FOREIGN KEY (`snapshot_id`) REFERENCES `conformancesnapshots`(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin;
-- Specification Groups.
CREATE TABLE `conformancesnapshotspecificationgroups` (
  `id` bigint NOT NULL,
  `sname` varchar(254) NOT NULL,
  `fname` varchar(254) NOT NULL,
  `description` text,
  `display_order` smallint NOT NULL DEFAULT '0',
  `snapshot_id` BIGINT NOT NULL,
  PRIMARY KEY (`id`,`snapshot_id`),
  KEY `cs_spg_id` (`id`),
  CONSTRAINT `cs_spg_fk_cs` FOREIGN KEY (`snapshot_id`) REFERENCES `conformancesnapshots`(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin;
-- Domains.
CREATE TABLE `conformancesnapshotdomains` (
  `id` bigint NOT NULL,
  `sname` varchar(254) NOT NULL,
  `fname` varchar(254) NOT NULL,
  `description` text,
  `snapshot_id` BIGINT NOT NULL,
  PRIMARY KEY (`id`,`snapshot_id`),
  KEY `cs_dom_id` (`id`),
  CONSTRAINT `cs_dom_fk_cs` FOREIGN KEY (`snapshot_id`) REFERENCES `conformancesnapshots`(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin;
-- Systems.
CREATE TABLE `conformancesnapshotsystems` (
  `id` bigint NOT NULL,
  `sname` varchar(254) NOT NULL,
  `fname` varchar(254) NOT NULL,
  `description` text,
  `api_key` varchar(254),
  `badge_key` varchar(254) NOT NULL,
  `snapshot_id` BIGINT NOT NULL,
  PRIMARY KEY (`id`,`snapshot_id`),
  KEY `cs_sys_id` (`id`),
  CONSTRAINT `cs_sys_fk_cs` FOREIGN KEY (`snapshot_id`) REFERENCES `conformancesnapshots`(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin;
-- Organisations.
CREATE TABLE `conformancesnapshotorganisations` (
  `id` bigint NOT NULL,
  `sname` varchar(254) NOT NULL,
  `fname` varchar(254) NOT NULL,
  `api_key` varchar(254),
  `snapshot_id` BIGINT NOT NULL,
  PRIMARY KEY (`id`,`snapshot_id`),
  KEY `cs_org_id` (`id`),
  CONSTRAINT `cs_org_fk_cs` FOREIGN KEY (`snapshot_id`) REFERENCES `conformancesnapshots`(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin;
--
-- MIGRATE DATA.
--
-- Test cases.
INSERT INTO `conformancesnapshottestcases`(`id`, `sname`, `fname`, `description`, `identifier`, `testsuite_order`, `is_optional`, `is_disabled`, `tags`, `spec_reference`, `spec_description`,  `spec_link`, `snapshot_id`)
SELECT DISTINCT `test_case_id`, `test_case`, `test_case`, `test_case_description`, `test_case_id`, `test_case_order`, `test_case_optional`, `test_case_disabled`, `test_case_tags`, `test_case_spec_reference`, `test_case_spec_description`,  `test_case_spec_link`, `snapshot_id`
FROM `conformancesnapshotresults`
WHERE `test_case_id` <= 0;

INSERT INTO `conformancesnapshottestcases`(`id`, `sname`, `fname`, `description`, `identifier`, `testsuite_order`, `is_optional`, `is_disabled`, `tags`, `spec_reference`, `spec_description`,  `spec_link`, `snapshot_id`)
SELECT DISTINCT `tc`.`id`, `tc`.`sname`, `tc`.`fname`, `tc`.`description`, `tc`.`identifier`, `tc`.`testsuite_order`, `tc`.`is_optional`, `tc`.`is_disabled`, `tc`.`tags`, `tc`.`spec_reference`, `tc`.`spec_description`,  `tc`.`spec_link`, `res`.`snapshot_id`
FROM `conformancesnapshotresults` AS `res`
JOIN `testcases` AS `tc` ON (`res`.`test_case_id` = `tc`.`id`)
WHERE `res`.`test_case_id` > 0;
-- Test suites.
INSERT INTO `conformancesnapshottestsuites`(`id`, `sname`, `fname`, `description`, `identifier`, `spec_reference`, `spec_description`, `spec_link`, `snapshot_id`)
SELECT DISTINCT `test_suite_id`, `test_suite`, `test_suite`, `test_suite_description`, `test_suite_id`, `test_suite_spec_reference`, `test_suite_spec_description`, `test_suite_spec_link`, `snapshot_id`
FROM `conformancesnapshotresults`
WHERE `test_suite_id` <= 0;

INSERT INTO `conformancesnapshottestsuites`(`id`, `sname`, `fname`, `description`, `identifier`, `spec_reference`, `spec_description`, `spec_link`, `snapshot_id`)
SELECT DISTINCT `ts`.`id`, `ts`.`sname`, `ts`.`fname`, `ts`.`description`, `ts`.`identifier`, `ts`.`spec_reference`, `ts`.`spec_description`, `ts`.`spec_link`, `res`.`snapshot_id`
FROM `conformancesnapshotresults` AS `res`
JOIN `testsuites` AS `ts` ON (`res`.`test_suite_id` = `ts`.`id`)
WHERE `res`.`test_suite_id` > 0;
-- Actors.
INSERT INTO `conformancesnapshotactors`(`id`, `actorId`, `name`, `description`, `api_key`, `snapshot_id`)
SELECT DISTINCT `actor_id`, `actor`, `actor`, null, `actor_id`, `snapshot_id`
FROM `conformancesnapshotresults`
WHERE `actor_id` <= 0;

INSERT INTO `conformancesnapshotactors`(`id`, `actorId`, `name`, `description`, `api_key`, `snapshot_id`)
SELECT DISTINCT `act`.`id`, `act`.`actorId`, `act`.`name`, `act`.`description`, `act`.`api_key`, `res`.`snapshot_id`
FROM `conformancesnapshotresults` AS `res`
JOIN `actors` AS `act` ON (`res`.`actor_id` = `act`.`id`)
WHERE `res`.`actor_id` > 0;
-- Specifications.
INSERT INTO `conformancesnapshotspecifications`(`id`, `sname`, `fname`, `description`, `display_order`, `api_key`, `snapshot_id`)
SELECT DISTINCT `spec_id`, `spec`, `spec`, null, `spec_display_order`, `spec_id`, `snapshot_id`
FROM `conformancesnapshotresults`
WHERE `spec_id` <= 0;

INSERT INTO `conformancesnapshotspecifications`(`id`, `sname`, `fname`, `description`, `display_order`, `api_key`, `snapshot_id`)
SELECT DISTINCT `sp`.`id`, `sp`.`sname`, `sp`.`fname`, `sp`.`description`, `sp`.`display_order`, `sp`.`api_key`, `res`.`snapshot_id`
FROM `conformancesnapshotresults` AS `res`
JOIN `specifications` AS `sp` ON (`res`.`spec_id` = `sp`.`id`)
WHERE `res`.`spec_id` > 0;
-- Specification Groups.
INSERT INTO `conformancesnapshotspecificationgroups`(`id`, `sname`, `fname`, `description`, `display_order`, `snapshot_id`)
SELECT DISTINCT `spec_group_id`, `spec_group`, `spec_group`, null, `spec_group_display_order`, `snapshot_id`
FROM `conformancesnapshotresults`
WHERE `spec_group_id` <= 0;

INSERT INTO `conformancesnapshotspecificationgroups`(`id`, `sname`, `fname`, `description`, `display_order`, `snapshot_id`)
SELECT DISTINCT `spg`.`id`, `spg`.`sname`, `spg`.`fname`, `spg`.`description`, `spg`.`display_order`, `res`.`snapshot_id`
FROM `conformancesnapshotresults` AS `res`
JOIN `specificationgroups` AS `spg` ON (`res`.`spec_group_id` = `spg`.`id`)
WHERE `res`.`spec_group_id` > 0;
-- Domains.
INSERT INTO `conformancesnapshotdomains`(`id`, `sname`, `fname`, `description`,`snapshot_id`)
SELECT DISTINCT `domain_id`, `domain`, `domain`, null, `snapshot_id`
FROM `conformancesnapshotresults`
WHERE `domain_id` <= 0;

INSERT INTO `conformancesnapshotdomains`(`id`, `sname`, `fname`, `description`, `snapshot_id`)
SELECT DISTINCT `dom`.`id`, `dom`.`sname`, `dom`.`fname`, `dom`.`description`, `res`.`snapshot_id`
FROM `conformancesnapshotresults` AS `res`
JOIN `domains` AS `dom` ON (`res`.`domain_id` = `dom`.`id`)
WHERE `res`.`domain_id` > 0;
-- Systems.
INSERT INTO `conformancesnapshotsystems`(`id`, `sname`, `fname`, `description`, `api_key`, `badge_key`, `snapshot_id`)
SELECT DISTINCT `sut_id`, `sut`, `sut`, null, null, `system_badge_key`, `snapshot_id`
FROM `conformancesnapshotresults`
WHERE `sut_id` <= 0;

INSERT INTO `conformancesnapshotsystems`(`id`, `sname`, `fname`, `description`, `api_key`, `badge_key`, `snapshot_id`)
SELECT DISTINCT `sys`.`id`, `sys`.`sname`, `sys`.`fname`, `sys`.`description`, `sys`.`api_key`, `sys`.`badge_key`, `res`.`snapshot_id`
FROM `conformancesnapshotresults` AS `res`
JOIN `systems` AS `sys` ON (`res`.`sut_id` = `sys`.`id`)
WHERE `res`.`sut_id` > 0;
-- Organisations.
INSERT INTO `conformancesnapshotnorganisations`(`id`, `sname`, `fname`, `api_key`, `snapshot_id`)
SELECT DISTINCT `organization_id`, `organization`, `organization`, null, `snapshot_id`
FROM `conformancesnapshotresults`
WHERE `organization_id` <= 0;

INSERT INTO `conformancesnapshotnorganisations`(`id`, `sname`, `fname`, `api_key`, `snapshot_id`)
SELECT DISTINCT `org`.`id`, `org`.`sname`, `org`.`fname`, `org`.`api_key`, `res`.`snapshot_id`
FROM `conformancesnapshotresults` AS `res`
JOIN `organizations` AS `org` ON (`res`.`organization_id` = `org`.`id`)
WHERE `res`.`organization_id` > 0;
--
-- DROP EXISTING COLUMNS.
--
ALTER TABLE `conformancesnapshotresults` DROP COLUMN `organization`;
ALTER TABLE `conformancesnapshotresults` DROP COLUMN `sut`;
ALTER TABLE `conformancesnapshotresults` DROP COLUMN `domain`;
ALTER TABLE `conformancesnapshotresults` DROP COLUMN `spec_group`;
ALTER TABLE `conformancesnapshotresults` DROP COLUMN `spec_group_display_order`;
ALTER TABLE `conformancesnapshotresults` DROP COLUMN `spec`;
ALTER TABLE `conformancesnapshotresults` DROP COLUMN `spec_display_order`;
ALTER TABLE `conformancesnapshotresults` DROP COLUMN `actor`;
ALTER TABLE `conformancesnapshotresults` DROP COLUMN `test_suite`;
ALTER TABLE `conformancesnapshotresults` DROP COLUMN `test_suite_description`;
ALTER TABLE `conformancesnapshotresults` DROP COLUMN `test_case`;
ALTER TABLE `conformancesnapshotresults` DROP COLUMN `test_case_description`;
ALTER TABLE `conformancesnapshotresults` DROP COLUMN `test_case_optional`;
ALTER TABLE `conformancesnapshotresults` DROP COLUMN `test_case_disabled`;
ALTER TABLE `conformancesnapshotresults` DROP COLUMN `test_case_tags`;
ALTER TABLE `conformancesnapshotresults` DROP COLUMN `test_case_order`;
ALTER TABLE `conformancesnapshotresults` DROP COLUMN `system_badge_key`;
ALTER TABLE `conformancesnapshotresults` DROP COLUMN `actor_api_key`;
ALTER TABLE `conformancesnapshotresults` DROP COLUMN `test_case_spec_link`;
ALTER TABLE `conformancesnapshotresults` DROP COLUMN `test_suite_spec_reference`;
ALTER TABLE `conformancesnapshotresults` DROP COLUMN `test_suite_spec_description`;
ALTER TABLE `conformancesnapshotresults` DROP COLUMN `test_suite_spec_link`;
ALTER TABLE `conformancesnapshotresults` DROP COLUMN `test_case_spec_reference`;
ALTER TABLE `conformancesnapshotresults` DROP COLUMN `test_case_spec_description`;
