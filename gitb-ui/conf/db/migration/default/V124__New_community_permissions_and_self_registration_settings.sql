ALTER TABLE `communities` ADD COLUMN `allow_user_management` TINYINT DEFAULT 1 NOT NULL;
ALTER TABLE `communities` ADD COLUMN `selfreg_allow_org_tokens` TINYINT DEFAULT 0 NOT NULL;
ALTER TABLE `communities` ADD COLUMN `selfreg_allow_org_token_management` TINYINT DEFAULT 0 NOT NULL;
ALTER TABLE `communities` ADD COLUMN `selfreg_force_org_token_input` TINYINT DEFAULT 0 NOT NULL;
ALTER TABLE `organizations` ADD COLUMN `selfreg_token` varchar(254);
CREATE INDEX `org_idx_selfreg_token` on `organizations`(`selfreg_token`);
