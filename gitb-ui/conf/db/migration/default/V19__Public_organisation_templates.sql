ALTER TABLE `organizations` ADD COLUMN `template` TINYINT DEFAULT 0 NOT NULL;
ALTER TABLE `organizations` ADD COLUMN `template_name` VARCHAR(254);