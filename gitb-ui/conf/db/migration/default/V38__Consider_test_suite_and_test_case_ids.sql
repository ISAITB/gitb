ALTER TABLE `testsuites` ADD COLUMN `identifier` varchar(254);
ALTER TABLE `testcases` ADD COLUMN `identifier` varchar(254);

DROP INDEX `ts_sn_vsn_si_idx` ON `testsuites`;

UPDATE `testsuites` SET `identifier` = `sname`;
UPDATE `testcases` SET `identifier` = `sname`;

ALTER TABLE `testsuites` MODIFY COLUMN `identifier` varchar(254) NOT NULL;
ALTER TABLE `testcases` MODIFY COLUMN `identifier` varchar(254) NOT NULL;

CREATE UNIQUE INDEX `ts_id_vsn_si_idx` ON `testsuites` (`identifier`,`version`,`specification`);