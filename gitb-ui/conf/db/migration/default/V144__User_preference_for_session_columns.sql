ALTER TABLE `userpreferences` ADD COLUMN `own_sessions` VARCHAR(255) DEFAULT '' NOT NULL;
ALTER TABLE `userpreferences` ADD COLUMN `all_sessions` VARCHAR(255) DEFAULT '' NOT NULL;
ALTER TABLE `userpreferencedefaults` ADD COLUMN `own_sessions` VARCHAR(255) DEFAULT '' NOT NULL;
ALTER TABLE `userpreferencedefaults` ADD COLUMN `all_sessions` VARCHAR(255) DEFAULT '' NOT NULL;
