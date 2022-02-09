ALTER TABLE `organizations` ADD COLUMN `api_key` varchar(254);
ALTER TABLE `systems` ADD COLUMN `api_key` varchar(254);
ALTER TABLE `organizations` ADD CONSTRAINT `unique_api_key` UNIQUE (`api_key`);
ALTER TABLE `systems` ADD CONSTRAINT `unique_api_key` UNIQUE (`api_key`);
CREATE INDEX `org_idx_api_key` on `organizations`(`api_key`);
CREATE INDEX `sys_idx_api_key` on `systems`(`api_key`);