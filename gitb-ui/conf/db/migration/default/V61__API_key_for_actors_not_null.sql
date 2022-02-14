ALTER TABLE `actors` MODIFY `api_key` VARCHAR(255) NOT NULL;
ALTER TABLE `actors` ADD CONSTRAINT `unique_api_key` UNIQUE (`api_key`);
CREATE INDEX `act_idx_api_key` on `actors`(`api_key`);