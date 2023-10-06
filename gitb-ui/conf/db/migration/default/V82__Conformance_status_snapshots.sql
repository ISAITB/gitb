-- Conformance snapshots table.
CREATE TABLE `conformancesnapshots` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `label` varchar(254) NOT NULL,
  `snapshot_time` TIMESTAMP NOT NULL,
  `community` BIGINT NOT NULL,
  PRIMARY KEY (`id`)
);
-- Association with communities
CREATE INDEX `conf_snap_idx_community` on `conformancesnapshots`(`community`);
ALTER TABLE `conformancesnapshots` ADD CONSTRAINT `conf_snap_fk_community` FOREIGN KEY (`community`) REFERENCES `communities`(`id`);

-- Conformance snapshot results table.
-- The ID columns are indexed for fast lookups but are not set as foreign keys. We need the IDs to be able to do correct groupings.
-- Also upon deletion the IDs are set to their negative value to distinguish them from values that can be used for lookups.
CREATE TABLE `conformancesnapshotresults` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `organization_id` bigint NOT NULL,
  `organization` varchar(254) COLLATE utf8mb4_bin NOT NULL,
  `sut_id` bigint NOT NULL,
  `sut` varchar(254) COLLATE utf8mb4_bin NOT NULL,
  `domain_id` bigint NOT NULL,
  `domain` varchar(254) COLLATE utf8mb4_bin NOT NULL,
  `spec_group_id` bigint DEFAULT NULL,
  `spec_group` varchar(254) COLLATE utf8mb4_bin DEFAULT NULL,
  `spec_group_display_order` smallint DEFAULT NULL,
  `spec_id` bigint NOT NULL,
  `spec` varchar(254) COLLATE utf8mb4_bin NOT NULL,
  `spec_display_order` smallint NOT NULL DEFAULT '0',
  `actor_id` bigint NOT NULL,
  `actor` varchar(254) COLLATE utf8mb4_bin NOT NULL,
  `test_suite_id` bigint NOT NULL,
  `test_suite` varchar(254) COLLATE utf8mb4_bin NOT NULL,
  `test_suite_description` text COLLATE utf8mb4_bin,
  `test_case_id` bigint NOT NULL,
  `test_case` varchar(254) COLLATE utf8mb4_bin NOT NULL,
  `test_case_description` text COLLATE utf8mb4_bin,
  `test_case_optional` smallint NOT NULL DEFAULT '0',
  `test_case_disabled` smallint NOT NULL DEFAULT '0',
  `test_case_tags` text COLLATE utf8mb4_bin,
  `test_case_order` smallint NOT NULL,
  `test_session_id` varchar(254) COLLATE utf8mb4_bin DEFAULT NULL,
  `result` varchar(254) COLLATE utf8mb4_bin NOT NULL,
  `output_message` text COLLATE utf8mb4_bin,
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
) ENGINE=InnoDB AUTO_INCREMENT=333 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin;