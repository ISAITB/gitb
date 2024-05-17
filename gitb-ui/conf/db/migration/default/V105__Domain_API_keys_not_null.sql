ALTER TABLE `domains` MODIFY `api_key` VARCHAR(255) NOT NULL;
ALTER TABLE `domains` ADD CONSTRAINT `unique_api_key` UNIQUE (`api_key`);
CREATE INDEX `dom_idx_api_key` on `domains`(`api_key`);