-- Modify table.
ALTER TABLE `conformancesnapshotsystems` ADD COLUMN `version` varchar(254) COLLATE utf8mb4_bin DEFAULT NULL;
-- Migrate.
UPDATE `conformancesnapshotsystems` SET `version` = (SELECT `version` FROM `systems` WHERE `systems`.`id` = `conformancesnapshotsystems`.`id`);