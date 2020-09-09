ALTER TABLE `communities` ADD COLUMN `allow_system_management` TINYINT DEFAULT 1 NOT NULL;
ALTER TABLE `communities` ADD COLUMN `allow_statement_management` TINYINT DEFAULT 1 NOT NULL;