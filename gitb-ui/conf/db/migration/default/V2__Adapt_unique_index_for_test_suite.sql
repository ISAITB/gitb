DROP INDEX `ts_sn_vsn_idx` ON `testsuites`;
CREATE UNIQUE INDEX `ts_sn_vsn_si_idx` ON `testsuites` (`sname`,`version`,`specification`);
