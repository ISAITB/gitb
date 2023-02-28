-- Mapping table from specifications to test suites
CREATE TABLE `specificationhastestsuites` (
  `spec` BIGINT NOT NULL,
  `testsuite` BIGINT NOT NULL,
  PRIMARY KEY (`spec`,`testsuite`)
);
ALTER TABLE `specificationhastestsuites` ADD CONSTRAINT `shts_fk_spec` FOREIGN KEY (`spec`) REFERENCES `specifications`(`id`);
ALTER TABLE `specificationhastestsuites` ADD CONSTRAINT `shts_fk_ts` FOREIGN KEY (`testsuite`) REFERENCES `testsuites`(`id`);
CREATE INDEX `shts_spec_idx` on `specificationhastestsuites`(`spec`);
CREATE INDEX `shts_ts_idx` on `specificationhastestsuites`(`testsuite`);

-- Add domain and shared flag to testsuites table
ALTER TABLE `testsuites` ADD COLUMN `domain` BIGINT;
ALTER TABLE `testsuites` ADD COLUMN `is_shared` TINYINT DEFAULT 0 NOT NULL;
ALTER TABLE `testsuites` ADD COLUMN `definition_path` VARCHAR(254);

-- Populate domain reference
UPDATE `testsuites` `ts`
INNER JOIN `specifications` `sp` ON `ts`.`specification` = `sp`.`id`
SET `ts`.`domain` = `sp`.`domain`;

-- Restrict domain on testsuites
ALTER TABLE `testsuites` MODIFY `domain` BIGINT NOT NULL;
ALTER TABLE `testsuites` ADD CONSTRAINT `ts_fk_domain` FOREIGN KEY (`domain`) REFERENCES `domains`(`id`);
CREATE INDEX `ts_idx_domain` on `testsuites`(`domain`);

-- Populate specificationhastestsuites.
INSERT INTO `specificationhastestsuites`(`spec`, `testsuite`) SELECT `specification`, `id` FROM `testsuites`;

-- Drop direct specification link from testsuites.
DROP INDEX `ts_id_vsn_si_idx` ON `testsuites`;
ALTER TABLE `testsuites` DROP FOREIGN KEY `tessui_fk_spe`;
ALTER TABLE `testsuites` DROP COLUMN `specification`;

-- Drop direct specification link from testcases.
ALTER TABLE `testcases` DROP FOREIGN KEY `tescas_fk_spe`;
ALTER TABLE `testcases` DROP COLUMN `target_spec`;