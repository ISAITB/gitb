ALTER TABLE `testresults` ADD COLUMN `domain_id` bigint(20) AFTER `sut_id`;
ALTER TABLE `testresults` ADD COLUMN `specification_id` bigint(20) AFTER `domain_id`;
ALTER TABLE `testresults` ADD COLUMN `testsuite_id` bigint(20) AFTER `actor_id`;
ALTER TABLE `testresults` ADD COLUMN `organization_id` bigint(20) AFTER `sut_id`;
ALTER TABLE `testresults` ADD COLUMN `community_id` bigint(20) AFTER `organization_id`;

ALTER TABLE `testresults` ADD COLUMN `domain` varchar(254) AFTER `domain_id`;
ALTER TABLE `testresults` ADD COLUMN `specification` varchar(254) AFTER `specification_id`;
ALTER TABLE `testresults` ADD COLUMN `testsuite` varchar(254) AFTER `testsuite_id`;
ALTER TABLE `testresults` ADD COLUMN `testcase` varchar(254) AFTER `testcase_id`;
ALTER TABLE `testresults` ADD COLUMN `organization` varchar(254) AFTER `organization_id`;
ALTER TABLE `testresults` ADD COLUMN `sut` varchar(254) AFTER `sut_id`;
ALTER TABLE `testresults` ADD COLUMN `community` varchar(254) AFTER `community_id`;
ALTER TABLE `testresults` ADD COLUMN `actor` varchar(254) AFTER `actor_id`;

ALTER TABLE `testresults` MODIFY `actor_id` bigint(20);
ALTER TABLE `testresults` MODIFY `sut_id` bigint(20);
ALTER TABLE `testresults` MODIFY `testcase_id` bigint(20);

update `testresults` set `organization_id` = (select `owner` from `systems` where `systems`.`id` = `testresults`.`sut_id`);
update `testresults` set `community_id` = (select `community` from `organizations` where `organizations`.`id` = `testresults`.`organization_id`);
update `testresults` set `testsuite_id` = (select `testsuite` from `testsuitehastestcases` where `testsuitehastestcases`.`testcase` = `testresults`.`testcase_id`);
update `testresults` set `specification_id` = (select `spec_id` from `specificationhasactors` where `specificationhasactors`.`actor_id` = `testresults`.`actor_id`);
update `testresults` set `domain_id` = (select `domain` from `actors` where `actors`.`id` = `testresults`.`actor_id`);

update `testresults` set `organization` = (select `sname` from `organizations` where `organizations`.`id` = `testresults`.`organization_id`);
update `testresults` set `testsuite` = (select `sname` from `testsuites` where `testsuites`.`id` = `testresults`.`testsuite_id`);
update `testresults` set `domain` = (select `sname` from `domains` where `domains`.`id` = `testresults`.`domain_id`);
update `testresults` set `sut` = (select `sname` from `systems` where `systems`.`id` = `testresults`.`sut_id`);
update `testresults` set `specification` = (select `sname` from `specifications` where `specifications`.`id` = `testresults`.`specification_id`);
update `testresults` set `testcase` = (select `sname` from `testcases` where `testcases`.`id` = `testresults`.`testcase_id`);
update `testresults` set `community` = (select `sname` from `communities` where `communities`.`id` = `testresults`.`community_id`);
update `testresults` set `actor` = (select `name` from `actors` where `actors`.`id` = `testresults`.`actor_id`);

ALTER TABLE `testresults` ADD CONSTRAINT `tr_fk_sut` FOREIGN KEY (`sut_id`) REFERENCES `systems`(`id`);
ALTER TABLE `testresults` ADD CONSTRAINT `tr_fk_organization` FOREIGN KEY (`organization_id`) REFERENCES `organizations`(`id`);
ALTER TABLE `testresults` ADD CONSTRAINT `tr_fk_community` FOREIGN KEY (`community_id`) REFERENCES `communities`(`id`);
ALTER TABLE `testresults` ADD CONSTRAINT `tr_fk_domain` FOREIGN KEY (`domain_id`) REFERENCES `domains`(`id`);
ALTER TABLE `testresults` ADD CONSTRAINT `tr_fk_specification` FOREIGN KEY (`specification_id`) REFERENCES `specifications`(`id`);
ALTER TABLE `testresults` ADD CONSTRAINT `tr_fk_testsuite` FOREIGN KEY (`testsuite_id`) REFERENCES `testsuites`(`id`);
ALTER TABLE `testresults` ADD CONSTRAINT `tr_fk_actor` FOREIGN KEY (`actor_id`) REFERENCES `actors`(`id`);

CREATE INDEX `tr_idx_sut` on `testresults`(`sut_id`);
CREATE INDEX `tr_idx_organization` on `testresults`(`organization_id`);
CREATE INDEX `tr_idx_community` on `testresults`(`community_id`);
CREATE INDEX `tr_idx_testcase` on `testresults`(`testcase_id`);
CREATE INDEX `tr_idx_testsuite` on `testresults`(`testsuite_id`);
CREATE INDEX `tr_idx_actor` on `testresults`(`actor_id`);
CREATE INDEX `tr_idx_specification` on `testresults`(`specification_id`);
CREATE INDEX `tr_idx_domain` on `testresults`(`domain_id`);

ALTER TABLE `testresults` DROP COLUMN `sut_version`;
