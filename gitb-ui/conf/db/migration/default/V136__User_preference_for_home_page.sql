ALTER TABLE `userpreferences` ADD COLUMN `home_page_type` SMALLINT DEFAULT 1 NOT NULL;
ALTER TABLE `userpreferencedefaults` ADD COLUMN `home_page_type` SMALLINT DEFAULT 1 NOT NULL;
