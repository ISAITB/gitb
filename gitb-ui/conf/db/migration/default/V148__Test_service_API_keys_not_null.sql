ALTER TABLE `testservices` MODIFY `api_key` VARCHAR(254) NOT NULL;
ALTER TABLE `testservices` ADD CONSTRAINT `unique_ts_api_key` UNIQUE (`api_key`);
CREATE INDEX `ts_idx_api_key` on `testservices`(`api_key`);
