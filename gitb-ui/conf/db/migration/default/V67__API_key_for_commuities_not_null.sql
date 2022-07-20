ALTER TABLE `communities` MODIFY `api_key` VARCHAR(255) NOT NULL;
ALTER TABLE `communities` ADD CONSTRAINT `unique_api_key` UNIQUE (`api_key`);
CREATE INDEX `com_idx_api_key` on `communities`(`api_key`);