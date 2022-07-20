ALTER TABLE `specifications` MODIFY `api_key` VARCHAR(255) NOT NULL;
ALTER TABLE `specifications` ADD CONSTRAINT `unique_api_key` UNIQUE (`api_key`);
CREATE INDEX `spe_idx_api_key` on `specifications`(`api_key`);