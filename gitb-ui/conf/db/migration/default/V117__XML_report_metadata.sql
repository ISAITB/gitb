ALTER TABLE `domains` ADD COLUMN `report_metadata` text COLLATE utf8mb4_bin;
ALTER TABLE `specifications` ADD COLUMN `report_metadata` text COLLATE utf8mb4_bin;
ALTER TABLE `specificationgroups` ADD COLUMN `report_metadata` text COLLATE utf8mb4_bin;
ALTER TABLE `actors` ADD COLUMN `report_metadata` text COLLATE utf8mb4_bin;

ALTER TABLE `conformancesnapshotdomains` ADD COLUMN `report_metadata` text COLLATE utf8mb4_bin;
ALTER TABLE `conformancesnapshotspecifications` ADD COLUMN `report_metadata` text COLLATE utf8mb4_bin;
ALTER TABLE `conformancesnapshotspecificationgroups` ADD COLUMN `report_metadata` text COLLATE utf8mb4_bin;
ALTER TABLE `conformancesnapshotactors` ADD COLUMN `report_metadata` text COLLATE utf8mb4_bin;