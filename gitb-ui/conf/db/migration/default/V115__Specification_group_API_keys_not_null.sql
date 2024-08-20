ALTER TABLE `specificationgroups` MODIFY `api_key` VARCHAR(255) NOT NULL;
ALTER TABLE `specificationgroups` ADD CONSTRAINT `unique_api_key` UNIQUE (`api_key`);
CREATE INDEX `sgroup_idx_api_key` on `specificationgroups`(`api_key`);