ALTER TABLE `systems` MODIFY `api_key` VARCHAR(254) NOT NULL;
-- Update snapshots
UPDATE `conformancesnapshotsystems` set `api_key` = (select `api_key` from `systems` where `systems`.`id` = `conformancesnapshotsystems`.`id`);
