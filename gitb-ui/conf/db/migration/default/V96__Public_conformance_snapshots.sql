ALTER TABLE `conformancesnapshots` ADD COLUMN `public_label` varchar(254) COLLATE utf8mb4_bin;
ALTER TABLE `conformancesnapshots` ADD COLUMN `is_public` TINYINT DEFAULT 0 NOT NULL;
ALTER TABLE `communities` ADD COLUMN `latest_status_label` varchar(254) COLLATE utf8mb4_bin;