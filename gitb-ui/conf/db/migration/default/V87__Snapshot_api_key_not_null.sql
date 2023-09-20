ALTER TABLE `conformancesnapshots` MODIFY `api_key` varchar(254) NOT NULL;
ALTER TABLE `conformancesnapshots` ADD CONSTRAINT `unique_api_key` UNIQUE (`api_key`);
CREATE INDEX `conf_snap_idx_api_key` on `conformancesnapshots`(`api_key`);