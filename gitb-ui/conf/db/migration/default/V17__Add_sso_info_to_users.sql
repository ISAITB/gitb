ALTER TABLE `users` ADD COLUMN `sso_uid` VARCHAR(254);
ALTER TABLE `users` ADD COLUMN `sso_email` VARCHAR(254);
ALTER TABLE `users` ADD COLUMN `sso_status` TINYINT DEFAULT 1 NOT NULL;

CREATE INDEX `users_idx_sso_uid` on `users`(`sso_uid`);