ALTER TABLE `testsuites` ADD COLUMN `spec_reference` varchar(254) COLLATE utf8mb4_bin;
ALTER TABLE `testsuites` ADD COLUMN `spec_description` text COLLATE utf8mb4_bin;
ALTER TABLE `testsuites` ADD COLUMN `spec_link` varchar(254) COLLATE utf8mb4_bin;
ALTER TABLE `testcases` ADD COLUMN `spec_reference` varchar(254) COLLATE utf8mb4_bin;
ALTER TABLE `testcases` ADD COLUMN `spec_description` text COLLATE utf8mb4_bin;
ALTER TABLE `testcases` ADD COLUMN `spec_link` varchar(254) COLLATE utf8mb4_bin;
ALTER TABLE `conformancesnapshotresults` ADD COLUMN `test_suite_spec_reference` varchar(254) COLLATE utf8mb4_bin;
ALTER TABLE `conformancesnapshotresults` ADD COLUMN `test_suite_spec_description` text COLLATE utf8mb4_bin;
ALTER TABLE `conformancesnapshotresults` ADD COLUMN `test_suite_spec_link` varchar(254) COLLATE utf8mb4_bin;
ALTER TABLE `conformancesnapshotresults` ADD COLUMN `test_case_spec_reference` varchar(254) COLLATE utf8mb4_bin;
ALTER TABLE `conformancesnapshotresults` ADD COLUMN `test_case_spec_description` text COLLATE utf8mb4_bin;
ALTER TABLE `conformancesnapshotresults` ADD COLUMN `test_case_spec_link` varchar(254) COLLATE utf8mb4_bin;