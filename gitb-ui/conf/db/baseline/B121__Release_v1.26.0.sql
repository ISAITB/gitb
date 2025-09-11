--
-- Table structure for table `domains`
--

CREATE TABLE `domains` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `sname` varchar(254) COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `fname` varchar(254) COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `api_key` varchar(254) COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `description` text COLLATE utf8mb4_0900_ai_ci,
  `report_metadata` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci,
  PRIMARY KEY (`id`),
  UNIQUE KEY `unique_api_key` (`api_key`),
  KEY `dom_idx_api_key` (`api_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

--
-- Table structure for table `specificationgroups`
--

CREATE TABLE `specificationgroups` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `sname` varchar(254) COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `fname` varchar(254) COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `api_key` varchar(255) COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `description` text COLLATE utf8mb4_0900_ai_ci,
  `report_metadata` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci,
  `domain` bigint NOT NULL,
  `display_order` smallint NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `unique_api_key` (`api_key`),
  KEY `spec_idx_domain` (`domain`),
  KEY `sgroup_idx_api_key` (`api_key`),
  CONSTRAINT `spec_group_fk_domain` FOREIGN KEY (`domain`) REFERENCES `domains` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

--
-- Table structure for table `specifications`
--

CREATE TABLE `specifications` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `sname` varchar(254) COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `fname` varchar(254) COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `description` text COLLATE utf8mb4_0900_ai_ci,
  `report_metadata` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci,
  `domain` bigint NOT NULL,
  `is_hidden` tinyint NOT NULL DEFAULT '0',
  `api_key` varchar(255) COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `spec_group` bigint DEFAULT NULL,
  `display_order` smallint NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `unique_api_key` (`api_key`),
  KEY `spe_fk_dom` (`domain`),
  KEY `spe_idx_api_key` (`api_key`),
  KEY `spec_idx_spec_group` (`spec_group`),
  CONSTRAINT `spe_fk_dom` FOREIGN KEY (`domain`) REFERENCES `domains` (`id`),
  CONSTRAINT `spec_fk_spec_group` FOREIGN KEY (`spec_group`) REFERENCES `specificationgroups` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

--
-- Table structure for table `actors`
--

CREATE TABLE `actors` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `actorId` varchar(254) COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `name` varchar(254) COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `description` text COLLATE utf8mb4_0900_ai_ci,
  `report_metadata` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci,
  `domain` bigint NOT NULL,
  `is_default` tinyint NOT NULL DEFAULT '0',
  `display_order` smallint DEFAULT NULL,
  `is_hidden` tinyint NOT NULL DEFAULT '0',
  `api_key` varchar(255) COLLATE utf8mb4_0900_ai_ci NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `unique_api_key` (`api_key`),
  KEY `ac_fk_dom` (`domain`),
  KEY `act_idx_api_key` (`api_key`),
  CONSTRAINT `ac_fk_dom` FOREIGN KEY (`domain`) REFERENCES `domains` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

--
-- Table structure for table `endpoints`
--

CREATE TABLE `endpoints` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(254) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `description` text COLLATE utf8mb4_0900_ai_ci,
  `actor` bigint NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `endp_act_unq_idx` (`name`,`actor`),
  KEY `end_fk_act` (`actor`),
  CONSTRAINT `end_fk_act` FOREIGN KEY (`actor`) REFERENCES `actors` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

--
-- Table structure for table `communities`
--

CREATE TABLE `communities` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `sname` varchar(254) COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `fname` varchar(254) COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `domain` bigint DEFAULT NULL,
  `support_email` varchar(254) COLLATE utf8mb4_0900_ai_ci DEFAULT NULL,
  `selfreg_type` tinyint NOT NULL DEFAULT '1',
  `selfreg_token` varchar(254) COLLATE utf8mb4_0900_ai_ci DEFAULT NULL,
  `selfreg_notification` tinyint NOT NULL DEFAULT '0',
  `description` text COLLATE utf8mb4_0900_ai_ci,
  `selfreg_restriction` tinyint NOT NULL DEFAULT '1',
  `selfreg_token_help_text` text COLLATE utf8mb4_0900_ai_ci,
  `selfreg_force_template` tinyint NOT NULL DEFAULT '0',
  `selfreg_force_properties` tinyint NOT NULL DEFAULT '0',
  `allow_certificate_download` tinyint NOT NULL DEFAULT '0',
  `allow_system_management` tinyint NOT NULL DEFAULT '1',
  `allow_statement_management` tinyint NOT NULL DEFAULT '1',
  `allow_post_test_org_updates` tinyint NOT NULL DEFAULT '1',
  `allow_post_test_sys_updates` tinyint NOT NULL DEFAULT '1',
  `allow_post_test_stm_updates` tinyint NOT NULL DEFAULT '1',
  `allow_automation_api` tinyint NOT NULL DEFAULT '0',
  `allow_community_view` tinyint NOT NULL DEFAULT '0',
  `api_key` varchar(255) COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `interaction_notification` tinyint NOT NULL DEFAULT '0',
  `latest_status_label` varchar(254) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `unique_api_key` (`api_key`),
  KEY `com_fk_dom` (`domain`),
  KEY `com_idx_api_key` (`api_key`),
  CONSTRAINT `com_fk_dom` FOREIGN KEY (`domain`) REFERENCES `domains` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

INSERT INTO `communities` VALUES (0,'Default community','Default community',NULL,NULL,1,NULL,0,NULL,1,NULL,0,0,0,1,1,1,1,1,0,0,'225D4900X6DA6X4479X9BBFX32B50FB1B916',0,NULL);
UPDATE `communities` SET `id` = 0;

--
-- Table structure for table `conformancesnapshots`
--

CREATE TABLE `conformancesnapshots` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `label` varchar(254) COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `snapshot_time` timestamp NOT NULL,
  `community` bigint NOT NULL,
  `api_key` varchar(254) COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `public_label` varchar(254) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL,
  `is_public` tinyint NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `unique_api_key` (`api_key`),
  KEY `conf_snap_idx_community` (`community`),
  KEY `conf_snap_idx_api_key` (`api_key`),
  CONSTRAINT `conf_snap_fk_community` FOREIGN KEY (`community`) REFERENCES `communities` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

--
-- Table structure for table `communitykeystores`
--

CREATE TABLE `communitykeystores` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `keystore_file` mediumblob NOT NULL,
  `keystore_type` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `keystore_pass` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `key_pass` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `community` bigint NOT NULL,
  PRIMARY KEY (`id`),
  KEY `cokey_fk_com` (`community`),
  CONSTRAINT `cokey_fk_com` FOREIGN KEY (`community`) REFERENCES `communities` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

--
-- Table structure for table `communitylabels`
--

CREATE TABLE `communitylabels` (
  `community` bigint NOT NULL,
  `label_type` tinyint NOT NULL,
  `singular_form` varchar(254) COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `plural_form` varchar(254) COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `fixed_case` tinyint NOT NULL DEFAULT '0',
  PRIMARY KEY (`community`,`label_type`),
  CONSTRAINT `cl_fk_com` FOREIGN KEY (`community`) REFERENCES `communities` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

--
-- Table structure for table `communityresources`
--

CREATE TABLE `communityresources` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(254) NOT NULL,
  `description` text,
  `community` bigint NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `cr_com_name_idx` (`name`,`community`),
  KEY `cr_fk_com` (`community`),
  CONSTRAINT `cr_fk_com` FOREIGN KEY (`community`) REFERENCES `communities` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

--
-- Table structure for table `conformancecertificates`
--

CREATE TABLE `conformancecertificates` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `title` varchar(255) COLLATE utf8mb4_0900_ai_ci DEFAULT NULL,
  `message` longtext COLLATE utf8mb4_0900_ai_ci,
  `include_message` tinyint NOT NULL DEFAULT '0',
  `include_test_status` tinyint NOT NULL DEFAULT '0',
  `include_test_cases` tinyint NOT NULL DEFAULT '0',
  `include_details` tinyint NOT NULL DEFAULT '0',
  `include_signature` tinyint NOT NULL DEFAULT '0',
  `community` bigint NOT NULL,
  `include_title` tinyint NOT NULL DEFAULT '1',
  `include_page_numbers` tinyint NOT NULL DEFAULT '1',
  PRIMARY KEY (`id`),
  UNIQUE KEY `cs_co_id_idx` (`community`),
  CONSTRAINT `ccert_fk_com` FOREIGN KEY (`community`) REFERENCES `communities` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

INSERT INTO `conformancecertificates` VALUES (1,'Conformance Certificate',NULL,0,1,1,1,0,0,1,1);

--
-- Table structure for table `conformanceoverviewcertificatemessages`
--

CREATE TABLE `conformanceoverviewcertificatemessages` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `message_type` smallint NOT NULL,
  `message` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `domain_id` bigint DEFAULT NULL,
  `group_id` bigint DEFAULT NULL,
  `specification_id` bigint DEFAULT NULL,
  `actor_id` bigint DEFAULT NULL,
  `community_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  KEY `cocertmsg_fk_dom` (`domain_id`),
  KEY `cocertmsg_fk_spg` (`group_id`),
  KEY `cocertmsg_fk_spe` (`specification_id`),
  KEY `cocertmsg_fk_act` (`actor_id`),
  KEY `cocertmsg_fk_com` (`community_id`),
  CONSTRAINT `cocertmsg_fk_act` FOREIGN KEY (`actor_id`) REFERENCES `actors` (`id`),
  CONSTRAINT `cocertmsg_fk_com` FOREIGN KEY (`community_id`) REFERENCES `communities` (`id`),
  CONSTRAINT `cocertmsg_fk_dom` FOREIGN KEY (`domain_id`) REFERENCES `domains` (`id`),
  CONSTRAINT `cocertmsg_fk_spe` FOREIGN KEY (`specification_id`) REFERENCES `specifications` (`id`),
  CONSTRAINT `cocertmsg_fk_spg` FOREIGN KEY (`group_id`) REFERENCES `specificationgroups` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

--
-- Table structure for table `conformanceoverviewcertificates`
--

CREATE TABLE `conformanceoverviewcertificates` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `title` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL,
  `message` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci,
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

--
-- Table structure for table `conformancesnapshotactors`
--

CREATE TABLE `conformancesnapshotactors` (
  `id` bigint NOT NULL,
  `actorId` varchar(254) COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `name` varchar(254) COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `description` text COLLATE utf8mb4_0900_ai_ci,
  `report_metadata` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci,
  `api_key` varchar(255) COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `snapshot_id` bigint NOT NULL,
  `visible` tinyint NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`,`snapshot_id`),
  KEY `cs_ac_id` (`id`),
  KEY `cs_ac_fk_cs` (`snapshot_id`),
  CONSTRAINT `cs_ac_fk_cs` FOREIGN KEY (`snapshot_id`) REFERENCES `conformancesnapshots` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

--
-- Table structure for table `conformancesnapshotcertificatemessages`
--

CREATE TABLE `conformancesnapshotcertificatemessages` (
  `snapshot_id` bigint NOT NULL,
  `message` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  PRIMARY KEY (`snapshot_id`),
  CONSTRAINT `cs_cert_msg_cs_fk` FOREIGN KEY (`snapshot_id`) REFERENCES `conformancesnapshots` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

--
-- Table structure for table `conformancesnapshotdomainparams`
--

CREATE TABLE `conformancesnapshotdomainparams` (
  `domain_id` bigint NOT NULL,
  `snapshot_id` bigint NOT NULL,
  `param_key` varchar(254) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `param_value` varchar(254) COLLATE utf8mb4_0900_ai_ci NOT NULL,
  PRIMARY KEY (`domain_id`,`snapshot_id`,`param_key`),
  KEY `cs_dom_param_cs_domain_idx` (`domain_id`),
  KEY `cs_dom_param_cs_snapshot_idx` (`snapshot_id`),
  CONSTRAINT `cs_dom_param_cs_fk` FOREIGN KEY (`snapshot_id`) REFERENCES `conformancesnapshots` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

--
-- Table structure for table `conformancesnapshotdomains`
--

CREATE TABLE `conformancesnapshotdomains` (
  `id` bigint NOT NULL,
  `sname` varchar(254) COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `fname` varchar(254) COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `description` text COLLATE utf8mb4_0900_ai_ci,
  `report_metadata` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci,
  `snapshot_id` bigint NOT NULL,
  PRIMARY KEY (`id`,`snapshot_id`),
  KEY `cs_dom_id` (`id`),
  KEY `cs_dom_fk_cs` (`snapshot_id`),
  CONSTRAINT `cs_dom_fk_cs` FOREIGN KEY (`snapshot_id`) REFERENCES `conformancesnapshots` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

--
-- Table structure for table `conformancesnapshotorganisations`
--

CREATE TABLE `conformancesnapshotorganisations` (
  `id` bigint NOT NULL,
  `sname` varchar(254) COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `fname` varchar(254) COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `api_key` varchar(254) COLLATE utf8mb4_0900_ai_ci DEFAULT NULL,
  `snapshot_id` bigint NOT NULL,
  PRIMARY KEY (`id`,`snapshot_id`),
  KEY `cs_org_id` (`id`),
  KEY `cs_org_fk_cs` (`snapshot_id`),
  CONSTRAINT `cs_org_fk_cs` FOREIGN KEY (`snapshot_id`) REFERENCES `conformancesnapshots` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

--
-- Table structure for table `conformancesnapshotorgparams`
--

CREATE TABLE `conformancesnapshotorgparams` (
  `org_id` bigint NOT NULL,
  `snapshot_id` bigint NOT NULL,
  `param_key` varchar(254) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `param_value` varchar(254) COLLATE utf8mb4_0900_ai_ci NOT NULL,
  PRIMARY KEY (`org_id`,`snapshot_id`,`param_key`),
  KEY `cs_org_param_cs_org_idx` (`org_id`),
  KEY `cs_org_param_cs_snapshot_idx` (`snapshot_id`),
  CONSTRAINT `cs_org_param_cs_fk` FOREIGN KEY (`snapshot_id`) REFERENCES `conformancesnapshots` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

--
-- Table structure for table `conformancesnapshotoverviewcertificatemessages`
--

CREATE TABLE `conformancesnapshotoverviewcertificatemessages` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `snapshot_id` bigint NOT NULL,
  `message_type` smallint NOT NULL,
  `message` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `domain_id` bigint DEFAULT NULL,
  `group_id` bigint DEFAULT NULL,
  `specification_id` bigint DEFAULT NULL,
  `actor_id` bigint DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `cs_cert_over_msg_cs_idx` (`snapshot_id`),
  CONSTRAINT `cs_cert_over_msg_cs_fk` FOREIGN KEY (`snapshot_id`) REFERENCES `conformancesnapshots` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

--
-- Table structure for table `conformancesnapshotspecificationgroups`
--

CREATE TABLE `conformancesnapshotspecificationgroups` (
  `id` bigint NOT NULL,
  `sname` varchar(254) COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `fname` varchar(254) COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `description` text COLLATE utf8mb4_0900_ai_ci,
  `report_metadata` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci,
  `display_order` smallint NOT NULL DEFAULT '0',
  `snapshot_id` bigint NOT NULL,
  PRIMARY KEY (`id`,`snapshot_id`),
  KEY `cs_spg_id` (`id`),
  KEY `cs_spg_fk_cs` (`snapshot_id`),
  CONSTRAINT `cs_spg_fk_cs` FOREIGN KEY (`snapshot_id`) REFERENCES `conformancesnapshots` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

--
-- Table structure for table `conformancesnapshotspecifications`
--

CREATE TABLE `conformancesnapshotspecifications` (
  `id` bigint NOT NULL,
  `sname` varchar(254) COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `fname` varchar(254) COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `description` text COLLATE utf8mb4_0900_ai_ci,
  `report_metadata` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci,
  `display_order` smallint NOT NULL DEFAULT '0',
  `api_key` varchar(255) COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `snapshot_id` bigint NOT NULL,
  PRIMARY KEY (`id`,`snapshot_id`),
  KEY `cs_sp_id` (`id`),
  KEY `cs_sp_fk_cs` (`snapshot_id`),
  CONSTRAINT `cs_sp_fk_cs` FOREIGN KEY (`snapshot_id`) REFERENCES `conformancesnapshots` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

--
-- Table structure for table `conformancesnapshotsysparams`
--

CREATE TABLE `conformancesnapshotsysparams` (
  `sys_id` bigint NOT NULL,
  `snapshot_id` bigint NOT NULL,
  `param_key` varchar(254) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `param_value` varchar(254) COLLATE utf8mb4_0900_ai_ci NOT NULL,
  PRIMARY KEY (`sys_id`,`snapshot_id`,`param_key`),
  KEY `cs_sys_param_cs_sys_idx` (`sys_id`),
  KEY `cs_sys_param_cs_snapshot_idx` (`snapshot_id`),
  CONSTRAINT `cs_sys_param_cs_fk` FOREIGN KEY (`snapshot_id`) REFERENCES `conformancesnapshots` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

--
-- Table structure for table `conformancesnapshotsystems`
--

CREATE TABLE `conformancesnapshotsystems` (
  `id` bigint NOT NULL,
  `sname` varchar(254) COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `fname` varchar(254) COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `version` varchar(254) COLLATE utf8mb4_0900_ai_ci DEFAULT NULL,
  `description` text COLLATE utf8mb4_0900_ai_ci,
  `api_key` varchar(254) COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `badge_key` varchar(254) COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `snapshot_id` bigint NOT NULL,
  PRIMARY KEY (`id`,`snapshot_id`),
  KEY `cs_sys_id` (`id`),
  KEY `cs_sys_fk_cs` (`snapshot_id`),
  CONSTRAINT `cs_sys_fk_cs` FOREIGN KEY (`snapshot_id`) REFERENCES `conformancesnapshots` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

--
-- Table structure for table `conformancesnapshottestcases`
--

CREATE TABLE `conformancesnapshottestcases` (
  `id` bigint NOT NULL,
  `sname` varchar(254) COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `fname` varchar(254) COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `version` varchar(254) COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT "",
  `description` text COLLATE utf8mb4_0900_ai_ci,
  `identifier` varchar(254) COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `testsuite_order` smallint NOT NULL,
  `is_optional` smallint NOT NULL DEFAULT '0',
  `is_disabled` smallint NOT NULL DEFAULT '0',
  `tags` text COLLATE utf8mb4_0900_ai_ci,
  `spec_reference` varchar(254) COLLATE utf8mb4_0900_ai_ci DEFAULT NULL,
  `spec_description` text COLLATE utf8mb4_0900_ai_ci,
  `spec_link` varchar(254) COLLATE utf8mb4_0900_ai_ci DEFAULT NULL,
  `snapshot_id` bigint NOT NULL,
  PRIMARY KEY (`id`,`snapshot_id`),
  KEY `cs_tc_id` (`id`),
  KEY `cs_tc_fk_cs` (`snapshot_id`),
  CONSTRAINT `cs_tc_fk_cs` FOREIGN KEY (`snapshot_id`) REFERENCES `conformancesnapshots` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

--
-- Table structure for table `conformancesnapshottestsuites`
--

CREATE TABLE `conformancesnapshottestsuites` (
  `id` bigint NOT NULL,
  `sname` varchar(254) COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `fname` varchar(254) COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `version` varchar(254) COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT "",
  `description` text COLLATE utf8mb4_0900_ai_ci,
  `identifier` varchar(254) COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `spec_reference` varchar(254) COLLATE utf8mb4_0900_ai_ci DEFAULT NULL,
  `spec_description` text COLLATE utf8mb4_0900_ai_ci,
  `spec_link` varchar(254) COLLATE utf8mb4_0900_ai_ci DEFAULT NULL,
  `snapshot_id` bigint NOT NULL,
  PRIMARY KEY (`id`,`snapshot_id`),
  KEY `cs_ts_id` (`id`),
  KEY `cs_ts_fk_cs` (`snapshot_id`),
  CONSTRAINT `cs_ts_fk_cs` FOREIGN KEY (`snapshot_id`) REFERENCES `conformancesnapshots` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

--
-- Table structure for table `domainparameters`
--

CREATE TABLE `domainparameters` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(254) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `description` text COLLATE utf8mb4_0900_ai_ci,
  `kind` varchar(254) COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `value` mediumblob NOT NULL,
  `domain` bigint NOT NULL,
  `in_tests` tinyint NOT NULL DEFAULT '1',
  `content_type` varchar(254) COLLATE utf8mb4_0900_ai_ci DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `dp_domain_name_idx` (`name`,`domain`),
  KEY `dp_idx_domain` (`domain`),
  CONSTRAINT `dp_fk_domain` FOREIGN KEY (`domain`) REFERENCES `domains` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

--
-- Table structure for table `endpointsupportstransactions`
--

CREATE TABLE `endpointsupportstransactions` (
  `actor` bigint NOT NULL,
  `endpoint` varchar(254) COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `transaction` varchar(254) COLLATE utf8mb4_0900_ai_ci NOT NULL,
  PRIMARY KEY (`actor`,`endpoint`,`transaction`),
  CONSTRAINT `endsuptra_fk_act` FOREIGN KEY (`actor`) REFERENCES `actors` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

--
-- Table structure for table `errortemplates`
--

CREATE TABLE `errortemplates` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(255) COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `content` longtext COLLATE utf8mb4_0900_ai_ci,
  `default_flag` tinyint NOT NULL DEFAULT '0',
  `description` longtext COLLATE utf8mb4_0900_ai_ci,
  `community` bigint NOT NULL,
  PRIMARY KEY (`id`),
  KEY `errtem_fk_com` (`community`),
  CONSTRAINT `errtem_fk_com` FOREIGN KEY (`community`) REFERENCES `communities` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

--
-- Table structure for table `landingpages`
--

CREATE TABLE `landingpages` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(255) COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `content` longtext COLLATE utf8mb4_0900_ai_ci,
  `default_flag` tinyint NOT NULL DEFAULT '0',
  `description` longtext COLLATE utf8mb4_0900_ai_ci,
  `community` bigint NOT NULL,
  PRIMARY KEY (`id`),
  KEY `lanpag_fk_com` (`community`),
  CONSTRAINT `lanpag_fk_com` FOREIGN KEY (`community`) REFERENCES `communities` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

--
-- Table structure for table `legalnotices`
--

CREATE TABLE `legalnotices` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(255) COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `content` longtext COLLATE utf8mb4_0900_ai_ci,
  `default_flag` tinyint NOT NULL DEFAULT '0',
  `description` longtext COLLATE utf8mb4_0900_ai_ci,
  `community` bigint NOT NULL,
  PRIMARY KEY (`id`),
  KEY `legnot_fk_com` (`community`),
  CONSTRAINT `legnot_fk_com` FOREIGN KEY (`community`) REFERENCES `communities` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

--
-- Table structure for table `options`
--

CREATE TABLE `options` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `sname` varchar(254) COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `fname` varchar(254) COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `description` text COLLATE utf8mb4_0900_ai_ci,
  `actor` bigint NOT NULL,
  PRIMARY KEY (`id`),
  KEY `opt_fk_act` (`actor`),
  CONSTRAINT `opt_fk_act` FOREIGN KEY (`actor`) REFERENCES `actors` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

--
-- Table structure for table `organisationparameters`
--

CREATE TABLE `organisationparameters` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(254) COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `test_key` varchar(254) COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `description` text COLLATE utf8mb4_0900_ai_ci,
  `use` varchar(254) COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `kind` varchar(254) COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `admin_only` tinyint NOT NULL DEFAULT '0',
  `not_for_tests` tinyint NOT NULL DEFAULT '1',
  `community` bigint NOT NULL,
  `in_exports` tinyint NOT NULL DEFAULT '0',
  `in_selfreg` tinyint NOT NULL DEFAULT '0',
  `hidden` tinyint NOT NULL DEFAULT '0',
  `allowed_values` longtext COLLATE utf8mb4_0900_ai_ci,
  `display_order` smallint NOT NULL DEFAULT '0',
  `depends_on` varchar(254) COLLATE utf8mb4_0900_ai_ci DEFAULT NULL,
  `depends_on_value` varchar(254) COLLATE utf8mb4_0900_ai_ci DEFAULT NULL,
  `default_value` text COLLATE utf8mb4_0900_ai_ci,
  PRIMARY KEY (`id`),
  KEY `op_idx_com` (`community`),
  CONSTRAINT `op_fk_com` FOREIGN KEY (`community`) REFERENCES `communities` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

--
-- Table structure for table `organizations`
--

CREATE TABLE `organizations` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `sname` varchar(254) COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `fname` varchar(254) COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `type` smallint NOT NULL,
  `admin_organization` tinyint NOT NULL DEFAULT '0',
  `landing_page` bigint DEFAULT NULL,
  `legal_notice` bigint DEFAULT NULL,
  `community` bigint NOT NULL,
  `error_template` bigint DEFAULT NULL,
  `template` tinyint NOT NULL DEFAULT '0',
  `template_name` varchar(254) COLLATE utf8mb4_0900_ai_ci DEFAULT NULL,
  `api_key` varchar(254) COLLATE utf8mb4_0900_ai_ci DEFAULT NULL,
  `updated_on` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `unique_api_key` (`api_key`),
  KEY `org_fk_com` (`community`),
  KEY `org_fk_lanpag` (`landing_page`),
  KEY `org_fk_legnot` (`legal_notice`),
  KEY `org_fk_errtem` (`error_template`),
  KEY `org_idx_api_key` (`api_key`),
  CONSTRAINT `org_fk_com` FOREIGN KEY (`community`) REFERENCES `communities` (`id`),
  CONSTRAINT `org_fk_errtem` FOREIGN KEY (`error_template`) REFERENCES `errortemplates` (`id`),
  CONSTRAINT `org_fk_lanpag` FOREIGN KEY (`landing_page`) REFERENCES `landingpages` (`id`),
  CONSTRAINT `org_fk_legnot` FOREIGN KEY (`legal_notice`) REFERENCES `legalnotices` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

INSERT INTO `organizations` VALUES (0,'Admin organisation','Admin organisation',1,1,NULL,NULL,0,NULL,0,NULL,NULL,CURRENT_TIMESTAMP());
UPDATE `organizations` SET `id` = 0;

--
-- Table structure for table `organisationparametervalues`
--

CREATE TABLE `organisationparametervalues` (
  `organisation` bigint NOT NULL,
  `parameter` bigint NOT NULL,
  `value` mediumblob NOT NULL,
  `content_type` varchar(254) COLLATE utf8mb4_0900_ai_ci DEFAULT NULL,
  PRIMARY KEY (`organisation`,`parameter`),
  KEY `opv_idx_org` (`organisation`),
  KEY `opv_idx_par` (`parameter`),
  CONSTRAINT `opv_fk_org` FOREIGN KEY (`organisation`) REFERENCES `organizations` (`id`),
  CONSTRAINT `opv_fk_par` FOREIGN KEY (`parameter`) REFERENCES `organisationparameters` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

--
-- Table structure for table `parameters`
--

CREATE TABLE `parameters` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(254) COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `description` text COLLATE utf8mb4_0900_ai_ci,
  `use` varchar(254) COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `kind` varchar(254) COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `endpoint` bigint NOT NULL,
  `admin_only` tinyint NOT NULL DEFAULT '0',
  `not_for_tests` tinyint NOT NULL DEFAULT '0',
  `hidden` tinyint NOT NULL DEFAULT '0',
  `allowed_values` longtext COLLATE utf8mb4_0900_ai_ci,
  `display_order` smallint NOT NULL DEFAULT '0',
  `depends_on` varchar(254) COLLATE utf8mb4_0900_ai_ci DEFAULT NULL,
  `depends_on_value` varchar(254) COLLATE utf8mb4_0900_ai_ci DEFAULT NULL,
  `test_key` varchar(254) COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `default_value` text COLLATE utf8mb4_0900_ai_ci,
  PRIMARY KEY (`id`),
  KEY `pa_fk_end` (`endpoint`),
  CONSTRAINT `pa_fk_end` FOREIGN KEY (`endpoint`) REFERENCES `endpoints` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

--
-- Table structure for table `processedarchives`
--

CREATE TABLE `processedarchives` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `archive_hash` varchar(254) NOT NULL,
  `import_time` timestamp NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `unique_archive_hash` (`archive_hash`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

--
-- Table structure for table `specificationhasactors`
--

CREATE TABLE `specificationhasactors` (
  `spec_id` bigint NOT NULL,
  `actor_id` bigint NOT NULL,
  PRIMARY KEY (`spec_id`,`actor_id`),
  KEY `spehact_fk_act` (`actor_id`),
  CONSTRAINT `spehact_fk_act` FOREIGN KEY (`actor_id`) REFERENCES `actors` (`id`),
  CONSTRAINT `spehact_fk_spe` FOREIGN KEY (`spec_id`) REFERENCES `specifications` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

--
-- Table structure for table `testsuites`
--

CREATE TABLE `testsuites` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `sname` varchar(254) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `fname` varchar(254) COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `version` varchar(254) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `authors` varchar(254) COLLATE utf8mb4_0900_ai_ci DEFAULT NULL,
  `original_date` varchar(254) COLLATE utf8mb4_0900_ai_ci DEFAULT NULL,
  `modification_date` varchar(254) COLLATE utf8mb4_0900_ai_ci DEFAULT NULL,
  `description` text COLLATE utf8mb4_0900_ai_ci,
  `keywords` varchar(254) COLLATE utf8mb4_0900_ai_ci DEFAULT NULL,
  `file_name` varchar(255) COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `has_documentation` tinyint NOT NULL DEFAULT '0',
  `documentation` longtext COLLATE utf8mb4_0900_ai_ci,
  `identifier` varchar(254) COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `is_hidden` tinyint NOT NULL DEFAULT '0',
  `domain` bigint NOT NULL,
  `is_shared` tinyint NOT NULL DEFAULT '0',
  `definition_path` varchar(254) COLLATE utf8mb4_0900_ai_ci DEFAULT NULL,
  `spec_reference` varchar(254) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL,
  `spec_description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci,
  `spec_link` varchar(254) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `ts_idx_domain` (`domain`),
  CONSTRAINT `ts_fk_domain` FOREIGN KEY (`domain`) REFERENCES `domains` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

--
-- Table structure for table `specificationhastestsuites`
--

CREATE TABLE `specificationhastestsuites` (
  `spec` bigint NOT NULL,
  `testsuite` bigint NOT NULL,
  PRIMARY KEY (`spec`,`testsuite`),
  KEY `shts_spec_idx` (`spec`),
  KEY `shts_ts_idx` (`testsuite`),
  CONSTRAINT `shts_fk_spec` FOREIGN KEY (`spec`) REFERENCES `specifications` (`id`),
  CONSTRAINT `shts_fk_ts` FOREIGN KEY (`testsuite`) REFERENCES `testsuites` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

--
-- Table structure for table `systemconfigurations`
--

CREATE TABLE `systemconfigurations` (
  `name` varchar(255) COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `parameter` longtext COLLATE utf8mb4_0900_ai_ci,
  `description` varchar(255) COLLATE utf8mb4_0900_ai_ci DEFAULT NULL,
  PRIMARY KEY (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

--
-- Dumping data for table `systemconfigurations`
--

INSERT INTO `systemconfigurations` VALUES ('session_alive_time','3600','session_alive_time');

--
-- Table structure for table `systems`
--

CREATE TABLE `systems` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `sname` varchar(254) COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `fname` varchar(254) COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `description` text COLLATE utf8mb4_0900_ai_ci,
  `version` varchar(254) COLLATE utf8mb4_0900_ai_ci DEFAULT NULL,
  `owner` bigint NOT NULL,
  `api_key` varchar(254) COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `badge_key` varchar(254) COLLATE utf8mb4_0900_ai_ci NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `unique_badge_key` (`badge_key`),
  UNIQUE KEY `unique_api_key` (`api_key`),
  KEY `sut_fk_org` (`owner`),
  KEY `sys_idx_api_key` (`api_key`),
  KEY `sys_idx_badge_key` (`badge_key`),
  CONSTRAINT `sut_fk_org` FOREIGN KEY (`owner`) REFERENCES `organizations` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

--
-- Table structure for table `users`
--

CREATE TABLE `users` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(254) COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `email` varchar(254) COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `password` varchar(254) COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `role` smallint NOT NULL,
  `organization` bigint NOT NULL,
  `onetime_password` tinyint NOT NULL DEFAULT '0',
  `sso_uid` varchar(254) COLLATE utf8mb4_0900_ai_ci DEFAULT NULL,
  `sso_email` varchar(254) COLLATE utf8mb4_0900_ai_ci DEFAULT NULL,
  `sso_status` tinyint NOT NULL DEFAULT '1',
  PRIMARY KEY (`id`),
  KEY `users_idx_sso_uid` (`sso_uid`),
  KEY `use_fk_org` (`organization`),
  CONSTRAINT `use_fk_org` FOREIGN KEY (`organization`) REFERENCES `organizations` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

--
-- Dumping data for table `users`
--

INSERT INTO `users` VALUES (1,'Administrator','admin@itb','',4,0,1,NULL,NULL,1);

--
-- Table structure for table `systemhasadmins`
--

CREATE TABLE `systemhasadmins` (
  `sut_id` bigint NOT NULL,
  `user_id` bigint NOT NULL,
  PRIMARY KEY (`sut_id`,`user_id`),
  KEY `suthadm_fk_use` (`user_id`),
  CONSTRAINT `suthadm_fk_sut` FOREIGN KEY (`sut_id`) REFERENCES `systems` (`id`),
  CONSTRAINT `suthadm_fk_use` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

--
-- Table structure for table `systemimplementsactors`
--

CREATE TABLE `systemimplementsactors` (
  `sut_id` bigint NOT NULL,
  `spec_id` bigint NOT NULL,
  `actor_id` bigint NOT NULL,
  PRIMARY KEY (`sut_id`,`spec_id`,`actor_id`),
  KEY `sutiact_fk_spe` (`spec_id`),
  KEY `sutiact_fk_act` (`actor_id`),
  CONSTRAINT `sutiact_fk_act` FOREIGN KEY (`actor_id`) REFERENCES `actors` (`id`),
  CONSTRAINT `sutiact_fk_spe` FOREIGN KEY (`spec_id`) REFERENCES `specifications` (`id`),
  CONSTRAINT `sutiact_fk_sut` FOREIGN KEY (`sut_id`) REFERENCES `systems` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

--
-- Table structure for table `systemimplementsoptions`
--

CREATE TABLE `systemimplementsoptions` (
  `sut_id` bigint NOT NULL,
  `option_id` bigint NOT NULL,
  PRIMARY KEY (`sut_id`,`option_id`),
  KEY `sutiopt_fk_opt` (`option_id`),
  CONSTRAINT `sutiopt_fk_opt` FOREIGN KEY (`option_id`) REFERENCES `options` (`id`),
  CONSTRAINT `sutiopt_fk_sut` FOREIGN KEY (`sut_id`) REFERENCES `systems` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

--
-- Table structure for table `systemparameters`
--

CREATE TABLE `systemparameters` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(254) COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `test_key` varchar(254) COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `description` text COLLATE utf8mb4_0900_ai_ci,
  `use` varchar(254) COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `kind` varchar(254) COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `admin_only` tinyint NOT NULL DEFAULT '0',
  `not_for_tests` tinyint NOT NULL DEFAULT '1',
  `community` bigint NOT NULL,
  `in_exports` tinyint NOT NULL DEFAULT '0',
  `hidden` tinyint NOT NULL DEFAULT '0',
  `allowed_values` longtext COLLATE utf8mb4_0900_ai_ci,
  `display_order` smallint NOT NULL DEFAULT '0',
  `depends_on` varchar(254) COLLATE utf8mb4_0900_ai_ci DEFAULT NULL,
  `depends_on_value` varchar(254) COLLATE utf8mb4_0900_ai_ci DEFAULT NULL,
  `default_value` text COLLATE utf8mb4_0900_ai_ci,
  PRIMARY KEY (`id`),
  KEY `sy_idx_com` (`community`),
  CONSTRAINT `sy_fk_com` FOREIGN KEY (`community`) REFERENCES `communities` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

--
-- Table structure for table `systemparametervalues`
--

CREATE TABLE `systemparametervalues` (
  `system` bigint NOT NULL,
  `parameter` bigint NOT NULL,
  `value` mediumblob NOT NULL,
  `content_type` varchar(254) COLLATE utf8mb4_0900_ai_ci DEFAULT NULL,
  PRIMARY KEY (`system`,`parameter`),
  KEY `spv_idx_sys` (`system`),
  KEY `spv_idx_par` (`parameter`),
  CONSTRAINT `spv_fk_par` FOREIGN KEY (`parameter`) REFERENCES `systemparameters` (`id`),
  CONSTRAINT `spv_fk_sys` FOREIGN KEY (`system`) REFERENCES `systems` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

--
-- Table structure for table `testcasegroups`
--

CREATE TABLE `testcasegroups` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `identifier` varchar(254) NOT NULL,
  `name` varchar(254) DEFAULT NULL,
  `description` text,
  `testsuite` bigint NOT NULL,
  PRIMARY KEY (`id`),
  KEY `tcg_ts_idx` (`testsuite`),
  CONSTRAINT `tcg_fk_testsuite` FOREIGN KEY (`testsuite`) REFERENCES `testsuites` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

--
-- Table structure for table `testcases`
--

CREATE TABLE `testcases` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `sname` varchar(254) COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `fname` varchar(254) COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `version` varchar(254) COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `authors` varchar(254) COLLATE utf8mb4_0900_ai_ci DEFAULT NULL,
  `original_date` varchar(254) COLLATE utf8mb4_0900_ai_ci DEFAULT NULL,
  `modification_date` varchar(254) COLLATE utf8mb4_0900_ai_ci DEFAULT NULL,
  `description` text COLLATE utf8mb4_0900_ai_ci,
  `keywords` varchar(254) COLLATE utf8mb4_0900_ai_ci DEFAULT NULL,
  `type` smallint NOT NULL,
  `path` varchar(254) COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `target_actors` varchar(254) COLLATE utf8mb4_0900_ai_ci DEFAULT NULL,
  `target_options` varchar(254) COLLATE utf8mb4_0900_ai_ci DEFAULT NULL,
  `testsuite_order` smallint NOT NULL,
  `has_documentation` tinyint NOT NULL DEFAULT '0',
  `documentation` longtext COLLATE utf8mb4_0900_ai_ci,
  `identifier` varchar(254) COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `is_optional` smallint NOT NULL DEFAULT '0',
  `is_disabled` smallint NOT NULL DEFAULT '0',
  `tags` text COLLATE utf8mb4_0900_ai_ci,
  `spec_reference` varchar(254) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL,
  `spec_description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci,
  `spec_link` varchar(254) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL,
  `testcase_group` bigint DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `tc_tcg_idx` (`testcase_group`),
  CONSTRAINT `tc_fk_tcg` FOREIGN KEY (`testcase_group`) REFERENCES `testcasegroups` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

--
-- Table structure for table `testcasecoversoptions`
--

CREATE TABLE `testcasecoversoptions` (
  `testcase` bigint NOT NULL,
  `option` bigint NOT NULL,
  PRIMARY KEY (`testcase`,`option`),
  KEY `tescovopt_fk_opt` (`option`),
  CONSTRAINT `tescovopt_fk_opt` FOREIGN KEY (`option`) REFERENCES `options` (`id`),
  CONSTRAINT `tescovopt_fk_tes` FOREIGN KEY (`testcase`) REFERENCES `testcases` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

--
-- Table structure for table `testcasehasactors`
--

CREATE TABLE `testcasehasactors` (
  `testcase` bigint NOT NULL,
  `specification` bigint NOT NULL,
  `actor` bigint NOT NULL,
  `sut` tinyint NOT NULL DEFAULT '0',
  PRIMARY KEY (`testcase`,`specification`,`actor`),
  KEY `teshact_fk_spe` (`specification`),
  KEY `teshact_fk_act` (`actor`),
  CONSTRAINT `teshact_fk_act` FOREIGN KEY (`actor`) REFERENCES `actors` (`id`),
  CONSTRAINT `teshact_fk_spe` FOREIGN KEY (`specification`) REFERENCES `specifications` (`id`),
  CONSTRAINT `teshact_fk_tes` FOREIGN KEY (`testcase`) REFERENCES `testcases` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

--
-- Table structure for table `testresults`
--

CREATE TABLE `testresults` (
  `test_session_id` varchar(254) COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `sut_id` bigint DEFAULT NULL,
  `sut` varchar(254) COLLATE utf8mb4_0900_ai_ci DEFAULT NULL,
  `organization_id` bigint DEFAULT NULL,
  `organization` varchar(254) COLLATE utf8mb4_0900_ai_ci DEFAULT NULL,
  `community_id` bigint DEFAULT NULL,
  `community` varchar(254) COLLATE utf8mb4_0900_ai_ci DEFAULT NULL,
  `domain_id` bigint DEFAULT NULL,
  `domain` varchar(254) COLLATE utf8mb4_0900_ai_ci DEFAULT NULL,
  `specification_id` bigint DEFAULT NULL,
  `specification` varchar(254) COLLATE utf8mb4_0900_ai_ci DEFAULT NULL,
  `actor_id` bigint DEFAULT NULL,
  `actor` varchar(254) COLLATE utf8mb4_0900_ai_ci DEFAULT NULL,
  `testsuite_id` bigint DEFAULT NULL,
  `testsuite` varchar(254) COLLATE utf8mb4_0900_ai_ci DEFAULT NULL,
  `testcase_id` bigint DEFAULT NULL,
  `testcase` varchar(254) COLLATE utf8mb4_0900_ai_ci DEFAULT NULL,
  `result` varchar(254) COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `start_time` timestamp NOT NULL DEFAULT '1970-01-01 00:00:01',
  `end_time` timestamp NULL DEFAULT NULL,
  `output_message` text COLLATE utf8mb4_0900_ai_ci,
  PRIMARY KEY (`test_session_id`),
  KEY `tr_idx_sut` (`sut_id`),
  KEY `tr_idx_organization` (`organization_id`),
  KEY `tr_idx_community` (`community_id`),
  KEY `tr_idx_testcase` (`testcase_id`),
  KEY `tr_idx_testsuite` (`testsuite_id`),
  KEY `tr_idx_actor` (`actor_id`),
  KEY `tr_idx_specification` (`specification_id`),
  KEY `tr_idx_domain` (`domain_id`),
  CONSTRAINT `tr_fk_actor` FOREIGN KEY (`actor_id`) REFERENCES `actors` (`id`),
  CONSTRAINT `tr_fk_community` FOREIGN KEY (`community_id`) REFERENCES `communities` (`id`),
  CONSTRAINT `tr_fk_domain` FOREIGN KEY (`domain_id`) REFERENCES `domains` (`id`),
  CONSTRAINT `tr_fk_organization` FOREIGN KEY (`organization_id`) REFERENCES `organizations` (`id`),
  CONSTRAINT `tr_fk_specification` FOREIGN KEY (`specification_id`) REFERENCES `specifications` (`id`),
  CONSTRAINT `tr_fk_sut` FOREIGN KEY (`sut_id`) REFERENCES `systems` (`id`),
  CONSTRAINT `tr_fk_testcase` FOREIGN KEY (`testcase_id`) REFERENCES `testcases` (`id`),
  CONSTRAINT `tr_fk_testsuite` FOREIGN KEY (`testsuite_id`) REFERENCES `testsuites` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

--
-- Table structure for table `teststepreports`
--

CREATE TABLE `teststepreports` (
  `test_session_id` varchar(254) COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `test_step_id` varchar(254) COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `result` smallint NOT NULL,
  `report_path` varchar(254) COLLATE utf8mb4_0900_ai_ci NOT NULL,
  PRIMARY KEY (`test_session_id`,`test_step_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

--
-- Table structure for table `testsuitehasactors`
--

CREATE TABLE `testsuitehasactors` (
  `testsuite` bigint NOT NULL,
  `actor` bigint NOT NULL,
  PRIMARY KEY (`testsuite`,`actor`),
  KEY `tessuihact_fk_act` (`actor`),
  CONSTRAINT `tessuihact_fk_act` FOREIGN KEY (`actor`) REFERENCES `actors` (`id`),
  CONSTRAINT `tessuihact_fk_tessui` FOREIGN KEY (`testsuite`) REFERENCES `testsuites` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

--
-- Table structure for table `testsuitehastestcases`
--

CREATE TABLE `testsuitehastestcases` (
  `testsuite` bigint NOT NULL,
  `testcase` bigint NOT NULL,
  PRIMARY KEY (`testsuite`,`testcase`),
  KEY `tessuihtescas_fk_tescas` (`testcase`),
  CONSTRAINT `tessuihtescas_fk_tescas` FOREIGN KEY (`testcase`) REFERENCES `testcases` (`id`),
  CONSTRAINT `tessuihtescas_fk_tessui` FOREIGN KEY (`testsuite`) REFERENCES `testsuites` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

--
-- Table structure for table `themes`
--

CREATE TABLE `themes` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `theme_key` varchar(254) NOT NULL,
  `description` text,
  `active` tinyint NOT NULL DEFAULT '0',
  `custom` tinyint NOT NULL DEFAULT '0',
  `separator_title_color` varchar(254) NOT NULL,
  `modal_title_color` varchar(254) NOT NULL,
  `table_title_color` varchar(254) NOT NULL,
  `card_title_color` varchar(254) NOT NULL,
  `page_title_color` varchar(254) NOT NULL,
  `heading_color` varchar(254) NOT NULL,
  `tab_link_color` varchar(254) NOT NULL,
  `footer_text_color` varchar(254) NOT NULL,
  `header_background_color` varchar(254) NOT NULL,
  `header_border_color` varchar(254) NOT NULL,
  `header_separator_color` varchar(254) NOT NULL,
  `header_logo_path` varchar(254) NOT NULL,
  `footer_background_color` varchar(254) NOT NULL,
  `footer_border_color` varchar(254) NOT NULL,
  `footer_logo_path` varchar(254) NOT NULL,
  `footer_logo_display` varchar(254) NOT NULL,
  `favicon_path` varchar(254) NOT NULL,
  `primary_btn_color` VARCHAR(254) NOT NULL,
  `primary_btn_label_color` VARCHAR(254) NOT NULL,
  `primary_btn_hover_color` VARCHAR(254) NOT NULL,
  `primary_btn_active_color` VARCHAR(254) NOT NULL,
  `secondary_btn_color` VARCHAR(254) NOT NULL,
  `secondary_btn_label_color` VARCHAR(254) NOT NULL,
  `secondary_btn_hover_color` VARCHAR(254) NOT NULL,
  `secondary_btn_active_color` VARCHAR(254) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `unique_theme_key` (`theme_key`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

INSERT INTO `themes` VALUES (1,'ec','European Commission theme.',0,0,'#337ab7','#337ab7','#337ab7','#337ab7','#FFFFFF','#337ab7','#337ab7','#FFFFFF','#0065a2','#074a8b','#FFFFFF','/assets/images/ec.png','#0065a2','#074a8b','/assets/images/gitb.png','inherit','/assets/images/favicon-ec.gif', '#337ab7', '#FFFFFF', '#2b689c', '#296292', '#6c757d', '#FFFFFF' , '#5c636a' , '#565e64'),(2,'gitb','Default GITB theme.',0,0,'#337ab7','#337ab7','#337ab7','#337ab7','#FFFFFF','#337ab7','#337ab7','#FFFFFF','#171717','#CCCCCC','#FFFFFF','/assets/images/gitb.png','#121214','#CCCCCC','/assets/images/gitb.png','none','/assets/images/favicon.png', '#337ab7', '#FFFFFF', '#2b689c', '#296292', '#6c757d', '#FFFFFF' , '#5c636a' , '#565e64');

--
-- Table structure for table `transactions`
--

CREATE TABLE `transactions` (
  `sname` varchar(254) COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `fname` varchar(254) COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `description` text COLLATE utf8mb4_0900_ai_ci,
  `domain` bigint NOT NULL,
  PRIMARY KEY (`sname`),
  KEY `tra_fk_dom` (`domain`),
  CONSTRAINT `tra_fk_dom` FOREIGN KEY (`domain`) REFERENCES `domains` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

--
-- Table structure for table `triggers`
--

CREATE TABLE `triggers` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(254) COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `description` text COLLATE utf8mb4_0900_ai_ci,
  `url` varchar(254) COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `event_type` tinyint NOT NULL,
  `operation` varchar(254) COLLATE utf8mb4_0900_ai_ci DEFAULT NULL,
  `active` tinyint NOT NULL DEFAULT '1',
  `latest_result_ok` tinyint DEFAULT NULL,
  `latest_result_output` text COLLATE utf8mb4_0900_ai_ci,
  `community` bigint NOT NULL,
  `service_type` tinyint NOT NULL DEFAULT '1',
  PRIMARY KEY (`id`),
  KEY `trigger_community_idx` (`community`),
  CONSTRAINT `trigger_fk_community` FOREIGN KEY (`community`) REFERENCES `communities` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

--
-- Table structure for table `triggerdata`
--

CREATE TABLE `triggerdata` (
  `data_type` tinyint NOT NULL,
  `data_id` bigint NOT NULL,
  `trigger` bigint NOT NULL,
  PRIMARY KEY (`data_type`,`data_id`,`trigger`),
  KEY `trigger_data_idx` (`trigger`),
  KEY `trigger_data_id_idx` (`data_id`),
  KEY `trigger_data_id_type_idx` (`data_id`,`data_type`),
  CONSTRAINT `triggerdata_fk_trigger` FOREIGN KEY (`trigger`) REFERENCES `triggers` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

--
-- Table structure for table `configurations`
--

CREATE TABLE `configurations` (
  `system` bigint NOT NULL,
  `parameter` bigint NOT NULL,
  `endpoint` bigint NOT NULL,
  `value` mediumblob NOT NULL,
  `content_type` varchar(254) COLLATE utf8mb4_0900_ai_ci DEFAULT NULL,
  PRIMARY KEY (`system`,`parameter`,`endpoint`),
  KEY `co_fk_sys` (`system`),
  KEY `co_fk_par` (`parameter`),
  KEY `co_fk_end` (`endpoint`),
  CONSTRAINT `co_fk_end` FOREIGN KEY (`endpoint`) REFERENCES `endpoints` (`id`),
  CONSTRAINT `co_fk_par` FOREIGN KEY (`parameter`) REFERENCES `parameters` (`id`),
  CONSTRAINT `co_fk_sys` FOREIGN KEY (`system`) REFERENCES `systems` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

--
-- Table structure for table `conformanceresults`
--

CREATE TABLE `conformanceresults` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `sut_id` bigint NOT NULL,
  `spec_id` bigint NOT NULL,
  `actor_id` bigint NOT NULL,
  `test_suite_id` bigint NOT NULL,
  `test_case_id` bigint NOT NULL,
  `test_session_id` varchar(254) COLLATE utf8mb4_0900_ai_ci DEFAULT NULL,
  `result` varchar(254) COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `output_message` text COLLATE utf8mb4_0900_ai_ci,
  `update_time` timestamp NULL DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `cr_resultinfo_idx` (`sut_id`,`spec_id`,`actor_id`,`test_suite_id`,`test_case_id`),
  KEY `cr_idx_sut` (`sut_id`),
  KEY `cr_idx_spec` (`spec_id`),
  KEY `cr_idx_actor` (`actor_id`),
  KEY `cr_idx_testsuite` (`test_suite_id`),
  KEY `cr_idx_testcase` (`test_case_id`),
  KEY `cr_idx_testsession` (`test_session_id`),
  CONSTRAINT `cr_fk_actor` FOREIGN KEY (`actor_id`) REFERENCES `actors` (`id`),
  CONSTRAINT `cr_fk_spec` FOREIGN KEY (`spec_id`) REFERENCES `specifications` (`id`),
  CONSTRAINT `cr_fk_sut` FOREIGN KEY (`sut_id`) REFERENCES `systems` (`id`),
  CONSTRAINT `cr_fk_testcase` FOREIGN KEY (`test_case_id`) REFERENCES `testcases` (`id`),
  CONSTRAINT `cr_fk_testsession` FOREIGN KEY (`test_session_id`) REFERENCES `testresults` (`test_session_id`),
  CONSTRAINT `cr_fk_testsuite` FOREIGN KEY (`test_suite_id`) REFERENCES `testsuites` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

--
-- Table structure for table `testinteractions`
--

CREATE TABLE `testinteractions` (
  `test_session_id` varchar(254) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `test_step_id` varchar(254) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `is_admin` tinyint NOT NULL DEFAULT '0',
  `tpl` blob NOT NULL,
  `created_on` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`test_session_id`,`test_step_id`),
  CONSTRAINT `ti_fk_tr` FOREIGN KEY (`test_session_id`) REFERENCES `testresults` (`test_session_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

--
-- Table structure for table `testresultdefinitions`
--

CREATE TABLE `testresultdefinitions` (
  `test_session_id` varchar(254) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `tpl` MEDIUMBLOB NOT NULL,
  PRIMARY KEY (`test_session_id`),
  CONSTRAINT `trd_fk_tr` FOREIGN KEY (`test_session_id`) REFERENCES `testresults` (`test_session_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

--
-- Table structure for table `conformancesnapshotresults`
--

CREATE TABLE `conformancesnapshotresults` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `organization_id` bigint NOT NULL,
  `sut_id` bigint NOT NULL,
  `domain_id` bigint NOT NULL,
  `spec_group_id` bigint DEFAULT NULL,
  `spec_id` bigint NOT NULL,
  `actor_id` bigint NOT NULL,
  `test_suite_id` bigint NOT NULL,
  `test_case_id` bigint NOT NULL,
  `test_session_id` varchar(254) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL,
  `result` varchar(254) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `output_message` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci,
  `update_time` timestamp NULL DEFAULT NULL,
  `snapshot_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  KEY `csr_idx_org` (`organization_id`),
  KEY `csr_idx_sut` (`sut_id`),
  KEY `csr_idx_domain` (`domain_id`),
  KEY `csr_idx_spec` (`spec_id`),
  KEY `csr_idx_actor` (`actor_id`),
  KEY `csr_idx_testsuite` (`test_suite_id`),
  KEY `csr_idx_testcase` (`test_case_id`),
  KEY `csr_idx_testsession` (`test_session_id`),
  KEY `csr_idx_snapshot` (`snapshot_id`),
  CONSTRAINT `crs_fk_snapshot` FOREIGN KEY (`snapshot_id`) REFERENCES `conformancesnapshots` (`id`),
  CONSTRAINT `crs_fk_testsession` FOREIGN KEY (`test_session_id`) REFERENCES `testresults` (`test_session_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

--
-- Table structure for table `communityreportsettings`
--

CREATE TABLE `communityreportsettings` (
  `report_type` smallint NOT NULL,
  `sign_pdf` tinyint NOT NULL DEFAULT '0',
  `custom_pdf` tinyint NOT NULL DEFAULT '0',
  `custom_pdf_with_custom_xml` tinyint NOT NULL DEFAULT '0',
  `custom_pdf_service` varchar(255) DEFAULT NULL,
  `community` bigint NOT NULL,
  PRIMARY KEY (`report_type`,`community`),
  KEY `crs_fk_com` (`community`),
  CONSTRAINT `crs_fk_com` FOREIGN KEY (`community`) REFERENCES `communities` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

--
-- Table structure for table `triggerfireexpressions`
--

CREATE TABLE `triggerfireexpressions` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `expression` varchar(1024) NOT NULL,
  `expression_type` smallint NOT NULL,
  `not_match` tinyint NOT NULL DEFAULT '0',
  `trigger` bigint NOT NULL,
  PRIMARY KEY (`id`),
  KEY `tfe_trigger_idx` (`trigger`),
  CONSTRAINT `tfe_fk_trigger` FOREIGN KEY (`trigger`) REFERENCES `triggers` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=113 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

--
-- Table structure for table `conformancesnapshottestcasegroups`
--

CREATE TABLE `conformancesnapshottestcasegroups` (
  `id` bigint NOT NULL,
  `identifier` varchar(254) COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `name` varchar(254) COLLATE utf8mb4_0900_ai_ci DEFAULT NULL,
  `description` text COLLATE utf8mb4_0900_ai_ci,
  `snapshot_id` bigint NOT NULL,
  PRIMARY KEY (`id`,`snapshot_id`),
  KEY `cs_tcg_id` (`id`),
  KEY `cs_tcg_fk_cs_idx` (`snapshot_id`),
  CONSTRAINT `cs_tcg_fk_cs` FOREIGN KEY (`snapshot_id`) REFERENCES `conformancesnapshots` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

--
-- Table structure for table `schema_version`
--

CREATE TABLE `schema_version` (
  `installed_rank` int NOT NULL,
  `version` varchar(50) COLLATE utf8mb4_0900_ai_ci DEFAULT NULL,
  `description` varchar(200) COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `type` varchar(20) COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `script` varchar(1000) COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `checksum` int DEFAULT NULL,
  `installed_by` varchar(100) COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `installed_on` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `execution_time` int NOT NULL,
  `success` tinyint NOT NULL,
  PRIMARY KEY (`installed_rank`),
  KEY `schema_version_s_idx` (`success`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

INSERT INTO `schema_version` VALUES
(1,'1','Base DB for GITB v1.1.0','SQL','V1__Base_DB_for_GITB_v1.1.0.sql',-73768430,'gitb',CURRENT_TIMESTAMP(),1,1),
(2,'2','Adapt unique index for test suite','SQL','V2__Adapt_unique_index_for_test_suite.sql',499898989,'gitb',CURRENT_TIMESTAMP(),1,1),
(3,'3','Link actors to specification','SQL','V3__Link_actors_to_specification.sql',831675058,'gitb',CURRENT_TIMESTAMP(),1,1),
(4,'4','Add test suite id to test case','SQL','V4__Add_test_suite_id_to_test_case.sql',-358339782,'gitb',CURRENT_TIMESTAMP(),1,1),
(5,'5','Add relations to test results','SQL','V5__Add_relations_to_test_results.sql',878432760,'gitb',CURRENT_TIMESTAMP(),1,1),
(6,'6','Add domain configuration','SQL','V6__Add_domain_configuration.sql',-1713576226,'gitb',CURRENT_TIMESTAMP(),1,1),
(7,'7','Add conformance results','SQL','V7__Add_conformance_results.sql',-1397307750,'gitb',CURRENT_TIMESTAMP(),1,1),
(8,'8','Add community support email','SQL','V8__Add_community_support_email.sql',360522625,'gitb',CURRENT_TIMESTAMP(),1,1),
(9,'9','Add support for onetime passwords','SQL','V9__Add_support_for_onetime_passwords.sql',-116732750,'gitb',CURRENT_TIMESTAMP(),1,1),
(10,'10','Add support for default actors and actor ordering','SQL','V10__Add_support_for default_actors_and_actor_ordering.sql',-1131698601,'gitb',CURRENT_TIMESTAMP(),1,1),
(11,'11','Default actor set by default','SQL','V11__Default_actor_set_by_default.sql',-1035269397,'gitb',CURRENT_TIMESTAMP(),1,1),
(12,'12','Add error templates','SQL','V12__Add_error_templates.sql',-925061201,'gitb',CURRENT_TIMESTAMP(),48,1),
(13,'13','Add conformance certificates','SQL','V13__Add_conformance_certificates.sql',41367143,'gitb',CURRENT_TIMESTAMP(),1,1),
(14,'14','Add test suite file name','SQL','V14__Add_test_suite_file_name.sql',740462335,'gitb',CURRENT_TIMESTAMP(),1,1),
(15,'15','Add test case order','SQL','V15__Add_test_case_order.sql',691562092,'gitb',CURRENT_TIMESTAMP(),1,1),
(16,'16','Add actor role to testcasehasactors','SQL','V16__Add_actor_role_to_testcasehasactors.sql',2095364803,'gitb',CURRENT_TIMESTAMP(),1,1),
(17,'17','Add sso info to users','SQL','V17__Add_sso_info_to_users.sql',1549358344,'gitb',CURRENT_TIMESTAMP(),1,1),
(18,'18','Self registration for communities','SQL','V18__Self_registration_for_communities.sql',-1831753832,'gitb',CURRENT_TIMESTAMP(),1,1),
(19,'19','Public organisation templates','SQL','V19__Public_organisation_templates.sql',-1939653405,'gitb',CURRENT_TIMESTAMP(),1,1),
(20,'20','Configuration parameter extensions','SQL','V20__Configuration_parameter_extensions.sql',-1035911279,'gitb',CURRENT_TIMESTAMP(),1,1),
(21,'21','Organisation and system properties','SQL','V21__Organisation_and_system_properties.sql',183476512,'gitb',CURRENT_TIMESTAMP(),1,1),
(22,'22','Convert blobs to mediumblobs','SQL','V22__Convert_blobs_to_mediumblobs.sql',-970297921,'gitb',CURRENT_TIMESTAMP(),1,1),
(23,'23','Custom properties exportable','SQL','V23__Custom_properties_exportable.sql',1153276711,'gitb',CURRENT_TIMESTAMP(),1,1),
(24,'24','Update collation for unique text columns','SQL','V24__Update_collation_for_unique_text_columns.sql',-2001633495,'gitb',CURRENT_TIMESTAMP(),1,1),
(25,'25','Custom community labels','SQL','V25__Custom_community_labels.sql',-2003050903,'gitb',CURRENT_TIMESTAMP(),1,1),
(26,'26','Custom community labels casing','SQL','V26__Custom_community_labels_casing.sql',-393528537,'gitb',CURRENT_TIMESTAMP(),1,1),
(27,'27','Add support for hidden actors','SQL','V27__Add_support_for_hidden_actors.sql',-87825788,'gitb',CURRENT_TIMESTAMP(),1,1),
(28,'28','Self registration notifications','SQL','V28__Self_registration_notifications.sql',840077450,'gitb',CURRENT_TIMESTAMP(),1,1),
(29,'29','Add support for hidden specifications','SQL','V29__Add_support_for_hidden_specifications.sql',-432389468,'gitb',CURRENT_TIMESTAMP(),1,1),
(30,'30','Community descriptions','SQL','V30__Community_descriptions.sql',1709179894,'gitb',CURRENT_TIMESTAMP(),1,1),
(31,'31','Organisation properties in selfreg','SQL','V31__Organisation_properties_in_selfreg.sql',254738404,'gitb',CURRENT_TIMESTAMP(),1,1),
(32,'32','Drop unused specification columns','SQL','V32__Drop_unused_specification_columns.sql',1746644882,'gitb',CURRENT_TIMESTAMP(),1,1),
(33,'33','Test suite and test case documentation','SQL','V33__Test_suite_and_test_case_documentation.sql',82696514,'gitb',CURRENT_TIMESTAMP(),1,1),
(34,'34','Community selfregistration restrictions','SQL','V34__Community_selfregistration_restrictions.sql',-343788971,'gitb',CURRENT_TIMESTAMP(),1,1),
(35,'35','Remove unlinked configurations','SQL','V35__Remove_unlinked_configurations.sql',-1308926168,'gitb',CURRENT_TIMESTAMP(),1,1),
(36,'36','Add FKs','SQL','V36__Add_FKs.sql',-1151636041,'gitb',CURRENT_TIMESTAMP(),1,1),
(37,'37','Custom self registration token help text','SQL','V37__Custom_self_registration_token_help_text.sql',1570543424,'gitb',CURRENT_TIMESTAMP(),1,1),
(38,'38','Consider test suite and test case ids','SQL','V38__Consider_test_suite_and_test_case_ids.sql',1771261602,'gitb',CURRENT_TIMESTAMP(),1,1),
(39,'39','Event triggers','SQL','V39__Event_triggers.sql',-1158885277,'gitb',CURRENT_TIMESTAMP(),1,1),
(40,'40','Custom properties hidden','SQL','V40__Custom_properties_hidden.sql',1141392030,'gitb',CURRENT_TIMESTAMP(),1,1),
(41,'41','self registration requirements','SQL','V41__self_registration_requirements.sql',1013755218,'gitb',CURRENT_TIMESTAMP(),1,1),
(42,'42','Property predefined values','SQL','V42__Property_predefined_values.sql',1422166555,'gitb',CURRENT_TIMESTAMP(),1,1),
(43,'43','Property dependencies and ordering','SQL','V43__Property_dependencies_and_ordering.sql',-1143442651,'gitb',CURRENT_TIMESTAMP(),1,1),
(44,'44','Certificate download by organisation users','SQL','V44__Certificate_download_by_organisation_users.sql',1813433776,'gitb',CURRENT_TIMESTAMP(),1,1),
(45,'45','Community permissions','SQL','V45__Community_permissions.sql',-285370422,'gitb',CURRENT_TIMESTAMP(),1,1),
(46,'46','Domain parameters not in tests','SQL','V46__Domain_parameters_not_in_tests.sql',1010371115,'gitb',CURRENT_TIMESTAMP(),1,1),
(47,'47','Community permissions linked to tests','SQL','V47__Community_permissions_linked_to_tests.sql',1459168953,'gitb',CURRENT_TIMESTAMP(),1,1),
(48,'48','Test session output','SQL','V48__Test_session_output.sql',2112471697,'gitb',CURRENT_TIMESTAMP(),1,1),
(49,'49','Hidden test suites','SQL','V49__Hidden_test_suites.sql',-1686810958,'gitb',CURRENT_TIMESTAMP(),1,1),
(50,'50','Force password change for default users','SQL','V50__Force_password_change_for_default_users.sql',-324785039,'gitb',CURRENT_TIMESTAMP(),1,1),
(51,'51','Update character sets and collations','SQL','V51__Update_character_sets_and_collations.sql',1922528259,'gitb',CURRENT_TIMESTAMP(),1,1),
(52,'52','Enable secrets verification','SQL','V52__Enable_secrets_verification.sql',-1713227413,'gitb',CURRENT_TIMESTAMP(),1,1),
(53,'53','Encrypt secrets at rest','JDBC','db.migration.default.V53__Encrypt_secrets_at_rest',NULL,'gitb',CURRENT_TIMESTAMP(),1,1),
(54,'54','Conformance result update time','SQL','V54__Conformance_result_update_time.sql',1260997359,'gitb',CURRENT_TIMESTAMP(),1,1),
(55,'55','Attachments','SQL','V55__Attachments.sql',-1574527087,'gitb',CURRENT_TIMESTAMP(),1,1),
(56,'56','Externalize attachments','JDBC','db.migration.default.V56__Externalize_attachments',NULL,'gitb',CURRENT_TIMESTAMP(),3,1),
(57,'57','API keys for organisation and system','SQL','V57__API_keys_for_organisation_and_system.sql',-601544797,'gitb',CURRENT_TIMESTAMP(),1,1),
(58,'58','automation api option for community','SQL','V58__automation_api_option_for_community.sql',1439458830,'gitb',CURRENT_TIMESTAMP(),1,1),
(59,'59','API key for actors','SQL','V59__API_key_for_actors.sql',1002432170,'gitb',CURRENT_TIMESTAMP(),1,1),
(60,'60','Set actor api keys','JDBC','db.migration.default.V60__Set_actor_api_keys',NULL,'gitb',CURRENT_TIMESTAMP(),1,1),
(61,'61','API key for actors not null','SQL','V61__API_key_for_actors_not_null.sql',-8356564,'gitb',CURRENT_TIMESTAMP(),1,1),
(62,'62','API key for specifications','SQL','V62__API_key_for_specifications.sql',1779994973,'gitb',CURRENT_TIMESTAMP(),1,1),
(63,'63','Set specification api keys','JDBC','db.migration.default.V63__Set_specification_api_keys',NULL,'gitb',CURRENT_TIMESTAMP(),1,1),
(64,'64','API key for specifications not null','SQL','V64__API_key_for_specifications_not_null.sql',140455792,'gitb',CURRENT_TIMESTAMP(),1,1),
(65,'65','API key for communities','SQL','V65__API_key_for_communities.sql',-809042688,'gitb',CURRENT_TIMESTAMP(),1,1),
(66,'66','Set community api keys','JDBC','db.migration.default.V66__Set_community_api_keys',NULL,'gitb',CURRENT_TIMESTAMP(),1,1),
(67,'67','API key for commuities not null','SQL','V67__API_key_for_commuities_not_null.sql',113682536,'gitb',CURRENT_TIMESTAMP(),1,1),
(68,'68','Trigger service types','SQL','V68__Trigger_service_types.sql',1463727509,'gitb',CURRENT_TIMESTAMP(),1,1),
(69,'69','Statement parameter names','SQL','V69__Statement_parameter_names.sql',1214434879,'gitb',CURRENT_TIMESTAMP(),1,1),
(70,'70','Parameter default values','SQL','V70__Parameter_default_values.sql',-1051843489,'gitb',CURRENT_TIMESTAMP(),1,1),
(71,'71','Community resources','SQL','V71__Community_resources.sql',-425769711,'gitb',CURRENT_TIMESTAMP(),1,1),
(72,'72','Test result definitions','SQL','V72__Test_result_definitions.sql',60376263,'gitb',CURRENT_TIMESTAMP(),1,1),
(73,'73','Shared test suites','SQL','V73__Shared_test_suites.sql',1945282748,'gitb',CURRENT_TIMESTAMP(),1,1),
(74,'74','Decouple test suite folders from specs','JDBC','db.migration.default.V74__Decouple_test_suite_folders_from_specs',NULL,'gitb',CURRENT_TIMESTAMP(),1,1),
(75,'75','Specification groups','SQL','V75__Specification_groups.sql',-1021339309,'gitb',CURRENT_TIMESTAMP(),1,1),
(76,'76','Optional system version','SQL','V76__Optional_system_version.sql',9240850,'gitb',CURRENT_TIMESTAMP(),1,1),
(77,'77','Specification ordering','SQL','V77__Specification_ordering.sql',149597996,'gitb',CURRENT_TIMESTAMP(),1,1),
(78,'78','Optional and disabled test cases','SQL','V78__Optional_and_disabled_test_cases.sql',-1570809626,'gitb',CURRENT_TIMESTAMP(),1,1),
(79,'79','Test case tags','SQL','V79__Test_case_tags.sql',1630356962,'gitb',CURRENT_TIMESTAMP(),1,1),
(80,'80','System configurations long values','SQL','V80__System_configurations_long_values.sql',-2143276474,'gitb',CURRENT_TIMESTAMP(),1,1),
(81,'81','Optional title in conformance certificate','SQL','V81__Optional_title_in_conformance_certificate.sql',-1139596533,'gitb',CURRENT_TIMESTAMP(),1,1),
(82,'82','Conformance status snapshots','SQL','V82__Conformance_status_snapshots.sql',-18038241,'gitb',CURRENT_TIMESTAMP(),1,1),
(83,'83','Conformance badges','SQL','V83__Conformance_badges.sql',183300382,'gitb',CURRENT_TIMESTAMP(),1,1),
(84,'84','Set system badge keys','JDBC','db.migration.default.V84__Set_system_badge_keys',NULL,'gitb',CURRENT_TIMESTAMP(),1,1),
(85,'85','Update snapshot badge keys','SQL','V85__Update_snapshot_badge_keys.sql',1119878505,'gitb',CURRENT_TIMESTAMP(),1,1),
(86,'86','Set conformance snapshot api keys','JDBC','db.migration.default.V86__Set_conformance_snapshot_api_keys',NULL,'gitb',CURRENT_TIMESTAMP(),1,1),
(87,'87','Snapshot api key not null','SQL','V87__Snapshot_api_key_not_null.sql',-1008012839,'gitb',CURRENT_TIMESTAMP(),1,1),
(88,'88','Themes','SQL','V88__Themes.sql',-1093466762,'gitb',CURRENT_TIMESTAMP(),1,1),
(89,'89','Sandbox archive hashes','SQL','V89__Sandbox_archive_hashes.sql',592072697,'gitb',CURRENT_TIMESTAMP(),1,1),
(90,'90','Optional conformance certificate page numbers','SQL','V90__Optional_conformance_certificate_page_numbers.sql',-1783357143,'gitb',CURRENT_TIMESTAMP(),1,1),
(91,'91','Delete sample user accounts','SQL','V91__Delete_sample_user_accounts.sql',1645928495,'gitb',CURRENT_TIMESTAMP(),1,1),
(92,'92','Organisation last update timestamp','SQL','V92__Organisation_last_update_timestamp.sql',968863300,'gitb',CURRENT_TIMESTAMP(),1,1),
(93,'93','Persistent test session user interactions','SQL','V93__Persistent_test_session_user_interactions.sql',2061585440,'gitb',CURRENT_TIMESTAMP(),1,1),
(94,'94','Pending interaction notifications','SQL','V94__Pending_interaction_notifications.sql',-746890738,'gitb',CURRENT_TIMESTAMP(),1,1),
(95,'95','Test suite specification references','SQL','V95__Test_suite_specification_references.sql',-2120813180,'gitb',CURRENT_TIMESTAMP(),1,1),
(96,'96','Public conformance snapshots','SQL','V96__Public_conformance_snapshots.sql',-2091534627,'gitb',CURRENT_TIMESTAMP(),1,1),
(97,'97','Extended conformance snapshot data','SQL','V97__Extended_conformance_snapshot_data.sql',1650461432,'gitb',CURRENT_TIMESTAMP(),1,1),
(98,'98','Conformance overview certificates','SQL','V98__Conformance_overview_certificates.sql',-458804731,'gitb',CURRENT_TIMESTAMP(),1,1),
(99,'99','SUT flag for snapshot actors','SQL','V99__SUT_flag_for_snapshot_actors.sql',-154362525,'gitb',CURRENT_TIMESTAMP(),1,1),
(100,'100','Remove builtin test organisation','SQL','V100__Remove_builtin_test_organisation.sql',-278512833,'gitb',CURRENT_TIMESTAMP(),1,1),
(101,'101','System version in snapshots','SQL','V101__System_version_in_snapshots.sql',-540628724,'gitb',CURRENT_TIMESTAMP(),1,1),
(102,'102','Test case and suite versions in snapshots','SQL','V102__Test_case_and_suite_versions_in_snapshots.sql',1541639576,'gitb',CURRENT_TIMESTAMP(),1,1),
(103,'103','Domain API keys','SQL','V103__Domain_API_keys.sql',1141368,'gitb',CURRENT_TIMESTAMP(),1,1),
(104,'104','Set domain api keys','JDBC','db.migration.default.V104__Set_domain_api_keys',NULL,'gitb',CURRENT_TIMESTAMP(),1,1),
(105,'105','Domain API keys not null','SQL','V105__Domain_API_keys_not_null.sql',223332356,'gitb',CURRENT_TIMESTAMP(),1,1),
(106,'106','Set system api keys','JDBC','db.migration.default.V106__Set_system_api_keys',NULL,'gitb',CURRENT_TIMESTAMP(),1,1),
(107,'107','System API keys not null','SQL','V107__System_API_keys_not_null.sql',1208983549,'gitb',CURRENT_TIMESTAMP(),1,1),
(108,'108','Set system api keys in snapshots','JDBC','db.migration.default.V108__Set_system_api_keys_in_snapshots',NULL,'gitb',CURRENT_TIMESTAMP(),1,1),
(109,'109','System API keys in snapshots not null','SQL','V109__System_API_keys_in_snapshots_not_null.sql',867885826,'gitb',CURRENT_TIMESTAMP(),1,1),
(110,'110','Use environment variables if no active theme set','JDBC','db.migration.default.V110__Use_environment_variables_if_no_active_theme_set',NULL,'gitb',CURRENT_TIMESTAMP(),1,1),
(111,'111','Button theming','SQL','V111__Button_theming.sql',-44417178,'gitb',CURRENT_TIMESTAMP(),1,1),
(112,'112','Delete stored default master password', 'SQL', 'V112__Delete_stored_default_master_password.sql', -215143895, 'gitb', CURRENT_TIMESTAMP(), 1, 1),
(113,'113','Specification group API keys', 'SQL', 'V113__Specification_group_API_keys.sql', -277162903, 'gitb', CURRENT_TIMESTAMP(), 1, 1),
(114,'114','Set specification group api keys', 'JDBC', 'db.migration.default.V114__Set_specification_group_api_keys', NULL, 'gitb', CURRENT_TIMESTAMP(), 1, 1),
(115,'115','Specification group API keys not null', 'SQL', 'V115__Specification_group_API_keys_not_null.sql', 416615516, 'gitb', CURRENT_TIMESTAMP(), 1, 1),
(116,'116','Report settings', 'SQL', 'V116__Report_settings.sql', -14344712, 'gitb', CURRENT_TIMESTAMP(), 1, 1),
(117,'117','XML report metadata', 'SQL', 'V117__XML_report_metadata.sql', -2009734919, 'gitb', CURRENT_TIMESTAMP(), 1, 1),
(118,'118','Trigger fire expressions', 'SQL', 'V118__Trigger_fire_expressions.sql', -763600425, 'gitb', CURRENT_TIMESTAMP(), 1, 1),
(119,'119','Test case groups', 'SQL', 'V119__Test_case_groups.sql', -319546087, 'gitb', CURRENT_TIMESTAMP(), 1, 1),
(120,'120',"TPL storage as MEDIUMBLOB",'SQL','V120__TPL_storage_as_MEDIUMBLOB.sql',1107412605,'gitb',CURRENT_TIMESTAMP(), 1, 1),
(121,'121',"Allow readonly community view for users",'SQL','V121__Allow_readonly_community_view_for_users.sql',-1480519436,'gitb',CURRENT_TIMESTAMP(), 1, 1);
