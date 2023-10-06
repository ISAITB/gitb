ALTER TABLE `systems` MODIFY `badge_key` VARCHAR(254) NOT NULL;
ALTER TABLE `systems` ADD CONSTRAINT `unique_badge_key` UNIQUE (`badge_key`);
CREATE INDEX `sys_idx_badge_key` on `systems`(`badge_key`);
-- Badge and API keys for snapshot lookups.
ALTER TABLE `conformancesnapshotresults` ADD COLUMN `system_badge_key` varchar(254);
ALTER TABLE `conformancesnapshotresults` ADD COLUMN `actor_api_key` varchar(254);
-- Populate API keys.
UPDATE `conformancesnapshotresults` set `system_badge_key` = (select `badge_key` from `systems` where `systems`.`id` = `conformancesnapshotresults`.`sut_id`);
UPDATE `conformancesnapshotresults` set `actor_api_key` = (select `api_key` from `actors` where `actors`.`id` = `conformancesnapshotresults`.`actor_id`);
-- Set API keys as mandatory and indexed.
ALTER TABLE `conformancesnapshotresults` MODIFY `system_badge_key` VARCHAR(254) NOT NULL;
ALTER TABLE `conformancesnapshotresults` MODIFY `actor_api_key` VARCHAR(254) NOT NULL;
CREATE INDEX `csr_idx_system_badge_key` on `conformancesnapshotresults`(`system_badge_key`);
CREATE INDEX `csr_idx_actor_api_key` on `conformancesnapshotresults`(`actor_api_key`);
-- Overall snapshot key.
ALTER TABLE `conformancesnapshots` ADD COLUMN `api_key` varchar(254);