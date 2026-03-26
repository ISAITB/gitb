ALTER TABLE `organizations` ADD COLUMN `selfreg_default` TINYINT DEFAULT 0 NOT NULL;
ALTER TABLE `communities` ADD COLUMN `selfreg_join_existing` TINYINT DEFAULT 0 NOT NULL;
UPDATE `communities` SET `selfreg_join_existing` = 1 WHERE `selfreg_force_org_token_input` = 1;
