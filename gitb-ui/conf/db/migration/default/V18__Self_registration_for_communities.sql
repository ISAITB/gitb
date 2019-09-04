ALTER TABLE `communities` ADD COLUMN `selfreg_type` TINYINT DEFAULT 1 NOT NULL;
ALTER TABLE `communities` ADD COLUMN `selfreg_token` VARCHAR(254);

