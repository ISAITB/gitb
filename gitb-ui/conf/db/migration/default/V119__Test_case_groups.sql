CREATE TABLE `testcasegroups` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `identifier` varchar(254) NOT NULL,
  `name` varchar(254),
  `description` text,
  `testsuite` BIGINT NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE INDEX `tcg_ts_idx` ON `testcasegroups` (`testsuite`);
ALTER TABLE `testcasegroups` ADD CONSTRAINT `tcg_fk_testsuite` FOREIGN KEY (`testsuite`) REFERENCES `testsuites`(`id`);

ALTER TABLE `testcases` ADD COLUMN `testcase_group` BIGINT NULL;
CREATE INDEX `tc_tcg_idx` ON `testcases` (`testcase_group`);
ALTER TABLE `testcases` ADD CONSTRAINT `tc_fk_tcg` FOREIGN KEY (`testcase_group`) REFERENCES `testcasegroups`(`id`);

CREATE TABLE `conformancesnapshottestcasegroups` (
  `id` BIGINT NOT NULL,
  `identifier` VARCHAR(254) NOT NULL,
  `name` VARCHAR(254),
  `description` text,
  `snapshot_id` BIGINT NOT NULL,
  PRIMARY KEY (`id`,`snapshot_id`),
  KEY `cs_tcg_id` (`id`),
  CONSTRAINT `cs_tcg_fk_cs` FOREIGN KEY (`snapshot_id`) REFERENCES `conformancesnapshots`(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin;

ALTER TABLE `conformancesnapshotresults` ADD COLUMN `test_case_group_id` BIGINT;