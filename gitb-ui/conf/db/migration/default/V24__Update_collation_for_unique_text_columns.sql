ALTER TABLE `endpoints` MODIFY `name` varchar(254) NOT NULL COLLATE utf8mb4_bin;
ALTER TABLE `domainparameters` MODIFY `name` varchar(254) NOT NULL COLLATE utf8mb4_bin;
ALTER TABLE `testsuites` MODIFY `sname` varchar(254) NOT NULL COLLATE utf8mb4_bin;
ALTER TABLE `testsuites` MODIFY `version` varchar(254) NOT NULL COLLATE utf8mb4_bin;
