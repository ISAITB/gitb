ALTER TABLE `testflags` ADD COLUMN `has_conformance_priority` TINYINT NOT NULL DEFAULT 0 AFTER `admin_only`;

CREATE INDEX `tr_idx_sut_testcase_start` ON `testresults` (`sut_id`, `testcase_id`, `start_time`);
