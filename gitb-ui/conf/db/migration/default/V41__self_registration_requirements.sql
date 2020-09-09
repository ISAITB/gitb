ALTER TABLE `communities` ADD COLUMN `selfreg_force_template` TINYINT DEFAULT 0 NOT NULL;
ALTER TABLE `communities` ADD COLUMN `selfreg_force_properties` TINYINT DEFAULT 0 NOT NULL;