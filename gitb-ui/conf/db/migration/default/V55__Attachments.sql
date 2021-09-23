ALTER TABLE `domainparameters` ADD COLUMN `content_type` varchar(254);
ALTER TABLE `organisationparametervalues` ADD COLUMN `content_type` varchar(254);
ALTER TABLE `systemparametervalues` ADD COLUMN `content_type` varchar(254);
ALTER TABLE `configurations` ADD COLUMN `content_type` varchar(254);
