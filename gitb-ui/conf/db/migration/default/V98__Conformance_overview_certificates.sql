--
-- Create conformance overview certificate table.
--
CREATE TABLE `conformanceoverviewcertificates` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `title` varchar(255) COLLATE utf8mb4_bin DEFAULT NULL,
  `message` longtext COLLATE utf8mb4_bin,
  `include_message` tinyint NOT NULL DEFAULT '0',
  `include_statement_status` tinyint NOT NULL DEFAULT '0',
  `include_statements` tinyint NOT NULL DEFAULT '0',
  `include_statement_details` tinyint NOT NULL DEFAULT '0',
  `include_details` tinyint NOT NULL DEFAULT '0',
  `include_signature` tinyint NOT NULL DEFAULT '0',
  `include_title` tinyint NOT NULL DEFAULT '1',
  `include_page_numbers` tinyint NOT NULL DEFAULT '1',
  `enable_all` tinyint NOT NULL DEFAULT '0',
  `enable_domain` tinyint NOT NULL DEFAULT '0',
  `enable_group` tinyint NOT NULL DEFAULT '0',
  `enable_specification` tinyint NOT NULL DEFAULT '0',
  `community` bigint NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `cos_co_id_idx` (`community`),
  CONSTRAINT `cocert_fk_com` FOREIGN KEY (`community`) REFERENCES `communities` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin;
--
-- Table to support conformance overview messages at different levels.
--
CREATE TABLE `conformanceoverviewcertificatemessages` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `message_type` smallint NOT NULL,
  `message` longtext COLLATE utf8mb4_bin NOT NULL,
  `domain_id` bigint DEFAULT NULL,
  `group_id` bigint DEFAULT NULL,
  `specification_id` bigint DEFAULT NULL,
  `actor_id` bigint DEFAULT NULL,
  `community_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  CONSTRAINT `cocertmsg_fk_dom` FOREIGN KEY (`domain_id`) REFERENCES `domains` (`id`),
  CONSTRAINT `cocertmsg_fk_spg` FOREIGN KEY (`group_id`) REFERENCES `specificationgroups` (`id`),
  CONSTRAINT `cocertmsg_fk_spe` FOREIGN KEY (`specification_id`) REFERENCES `specifications` (`id`),
  CONSTRAINT `cocertmsg_fk_act` FOREIGN KEY (`actor_id`) REFERENCES `actors` (`id`),
  CONSTRAINT `cocertmsg_fk_com` FOREIGN KEY (`community_id`) REFERENCES `communities` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin;
--
-- Include domain parameter text values in snapshots.
--
CREATE TABLE `conformancesnapshotdomainparams` (
  `domain_id` bigint NOT NULL,
  `snapshot_id` bigint NOT NULL,
  `param_key` varchar(254) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL,
  `param_value` varchar(254) NOT NULL,
  PRIMARY KEY (`domain_id`,`snapshot_id`,`param_key`),
  KEY `cs_dom_param_cs_domain_idx` (`domain_id`),
  KEY `cs_dom_param_cs_snapshot_idx` (`snapshot_id`),
  CONSTRAINT `cs_dom_param_cs_fk` FOREIGN KEY (`snapshot_id`) REFERENCES `conformancesnapshots`(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin;
--
-- Migrate domain parameters text values for existing snapshots.
--
INSERT INTO `conformancesnapshotdomainparams`(`domain_id`, `snapshot_id`, `param_key`, `param_value`)
SELECT `snap`.`id`, `snap`.`snapshot_id`, `param`.`name`, CONVERT(`param`.`value` USING utf8mb4)
FROM `domainparameters` AS `param`
JOIN `conformancesnapshotdomains` AS `snap` ON (`param`.`domain` = `snap`.`id`)
WHERE `snap`.`id` > 0 AND `param`.`kind` = 'SIMPLE';
--
-- Include organisation parameter text values in snapshots.
--
CREATE TABLE `conformancesnapshotorgparams` (
  `org_id` bigint NOT NULL,
  `snapshot_id` bigint NOT NULL,
  `param_key` varchar(254) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL,
  `param_value` varchar(254) NOT NULL,
  PRIMARY KEY (`org_id`,`snapshot_id`,`param_key`),
  KEY `cs_org_param_cs_org_idx` (`org_id`),
  KEY `cs_org_param_cs_snapshot_idx` (`snapshot_id`),
  CONSTRAINT `cs_org_param_cs_fk` FOREIGN KEY (`snapshot_id`) REFERENCES `conformancesnapshots`(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin;
--
-- Migrate organisation parameters text values for existing snapshots.
--
INSERT INTO `conformancesnapshotorgparams`(`org_id`, `snapshot_id`, `param_key`, `param_value`)
SELECT `snap`.`id`, `snap`.`snapshot_id`, `paramdef`.`test_key`, CONVERT(`paramval`.`value` USING utf8mb4)
FROM `organisationparametervalues` AS `paramval`
JOIN `organisationparameters` AS `paramdef` ON (`paramval`.`parameter` = `paramdef`.`id`)
JOIN `conformancesnapshotorganisations` AS `snap` ON (`snap`.`id` = `paramval`.`organisation`)
WHERE `snap`.`id` > 0 AND `paramdef`.`kind` = 'SIMPLE';
--
-- Include system parameter text values in snapshots.
--
CREATE TABLE `conformancesnapshotsysparams` (
  `sys_id` bigint NOT NULL,
  `snapshot_id` bigint NOT NULL,
  `param_key` varchar(254) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL,
  `param_value` varchar(254) NOT NULL,
  PRIMARY KEY (`sys_id`,`snapshot_id`,`param_key`),
  KEY `cs_sys_param_cs_sys_idx` (`sys_id`),
  KEY `cs_sys_param_cs_snapshot_idx` (`snapshot_id`),
  CONSTRAINT `cs_sys_param_cs_fk` FOREIGN KEY (`snapshot_id`) REFERENCES `conformancesnapshots`(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin;
--
-- Migrate system parameters text values for existing snapshots.
--
INSERT INTO `conformancesnapshotsysparams`(`sys_id`, `snapshot_id`, `param_key`, `param_value`)
SELECT `snap`.`id`, `snap`.`snapshot_id`, `paramdef`.`test_key`, CONVERT(`paramval`.`value` USING utf8mb4)
FROM `systemparametervalues` AS `paramval`
JOIN `systemparameters` AS `paramdef` ON (`paramval`.`parameter` = `paramdef`.`id`)
JOIN `conformancesnapshotsystems` AS `snap` ON (`snap`.`id` = `paramval`.`system`)
WHERE `snap`.`id` > 0 AND `paramdef`.`kind` = 'SIMPLE';
--
-- Include conformance certificate setting messages in snapshots.
--
CREATE TABLE `conformancesnapshotcertificatemessages` (
  `snapshot_id` bigint NOT NULL,
  `message` longtext COLLATE utf8mb4_bin NOT NULL,
  PRIMARY KEY (`snapshot_id`),
  CONSTRAINT `cs_cert_msg_cs_fk` FOREIGN KEY (`snapshot_id`) REFERENCES `conformancesnapshots`(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin;

CREATE TABLE `conformancesnapshotoverviewcertificatemessages` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `snapshot_id` bigint NOT NULL,
  `message_type` smallint NOT NULL,
  `message` longtext COLLATE utf8mb4_bin NOT NULL,
  `domain_id` bigint DEFAULT NULL,
  `group_id` bigint DEFAULT NULL,
  `specification_id` bigint DEFAULT NULL,
  `actor_id` bigint DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `cs_cert_over_msg_cs_idx` (`snapshot_id`),
  CONSTRAINT `cs_cert_over_msg_cs_fk` FOREIGN KEY (`snapshot_id`) REFERENCES `conformancesnapshots`(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin;
--
-- Migrate messages for existing snapshots.
--
INSERT INTO `conformancesnapshotcertificatemessages`(`snapshot_id`, `message`)
SELECT `snap`.`id`, `cert`.`message`
FROM `conformancecertificates` AS `cert`
JOIN `conformancesnapshots` AS `snap` ON (`snap`.`community` = `cert`.`community`)
WHERE `cert`.`message` IS NOT NULL;
--
-- Extract certificate signatures into their own table
--
CREATE TABLE `communitykeystores` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `keystore_file` mediumblob NOT NULL,
  `keystore_type` varchar(255) COLLATE utf8mb4_bin NOT NULL,
  `keystore_pass` varchar(255) COLLATE utf8mb4_bin NOT NULL,
  `key_pass` varchar(255) COLLATE utf8mb4_bin NOT NULL,
  `community` bigint NOT NULL,
  PRIMARY KEY (`id`),
  CONSTRAINT `cokey_fk_com` FOREIGN KEY (`community`) REFERENCES `communities` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin;
--
-- Populate from existing certificate information.
--
INSERT INTO `communitykeystores`(`keystore_file`, `keystore_type`, `keystore_pass`, `key_pass`, `community`)
SELECT `cert`.`keystore_file`, `cert`.`keystore_type`, `cert`.`keystore_pass`, `cert`.`key_pass`, `cert`.`community`
FROM `conformancecertificates` AS `cert`
WHERE `cert`.`keystore_pass` IS NOT NULL;
--
-- Drop previous columns.
--
ALTER TABLE `conformancecertificates` DROP COLUMN `keystore_file`;
ALTER TABLE `conformancecertificates` DROP COLUMN `keystore_type`;
ALTER TABLE `conformancecertificates` DROP COLUMN `keystore_pass`;
ALTER TABLE `conformancecertificates` DROP COLUMN `key_pass`;