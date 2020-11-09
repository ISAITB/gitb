ALTER TABLE `communities` ADD COLUMN `allow_post_test_org_updates` TINYINT DEFAULT 1 NOT NULL;
ALTER TABLE `communities` ADD COLUMN `allow_post_test_sys_updates` TINYINT DEFAULT 1 NOT NULL;
ALTER TABLE `communities` ADD COLUMN `allow_post_test_stm_updates` TINYINT DEFAULT 1 NOT NULL;