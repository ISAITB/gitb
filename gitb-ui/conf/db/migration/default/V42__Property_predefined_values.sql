ALTER TABLE `organisationparameters` ADD COLUMN `allowed_values` LONGTEXT;
ALTER TABLE `systemparameters` ADD COLUMN `allowed_values` LONGTEXT;
ALTER TABLE `parameters` ADD COLUMN `allowed_values` LONGTEXT;
