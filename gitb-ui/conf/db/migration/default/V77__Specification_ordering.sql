ALTER TABLE `specifications` ADD COLUMN `display_order` SMALLINT DEFAULT 0 NOT NULL;
ALTER TABLE `specificationgroups` ADD COLUMN `display_order` SMALLINT DEFAULT 0 NOT NULL;