ALTER TABLE `organisationparameters` ADD COLUMN `display_order` SMALLINT(6) DEFAULT 0 NOT NULL;
ALTER TABLE `organisationparameters` ADD COLUMN `depends_on` varchar(254);
ALTER TABLE `organisationparameters` ADD COLUMN `depends_on_value` varchar(254);
ALTER TABLE `systemparameters` ADD COLUMN `display_order` SMALLINT(6) DEFAULT 0 NOT NULL;
ALTER TABLE `systemparameters` ADD COLUMN `depends_on` varchar(254);
ALTER TABLE `systemparameters` ADD COLUMN `depends_on_value` varchar(254);
ALTER TABLE `parameters` ADD COLUMN `display_order` SMALLINT(6) DEFAULT 0 NOT NULL;
ALTER TABLE `parameters` ADD COLUMN `depends_on` varchar(254);
ALTER TABLE `parameters` ADD COLUMN `depends_on_value` varchar(254);
