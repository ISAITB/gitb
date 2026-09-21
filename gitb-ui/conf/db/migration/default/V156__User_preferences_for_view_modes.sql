ALTER TABLE `userpreferences` ADD COLUMN `statements_list_view` TINYINT DEFAULT 0 NOT NULL;
ALTER TABLE `userpreferences` ADD COLUMN `messages_split_view` TINYINT DEFAULT 0 NOT NULL;
ALTER TABLE `userpreferencedefaults` ADD COLUMN `statements_list_view` TINYINT DEFAULT 0 NOT NULL;
ALTER TABLE `userpreferencedefaults` ADD COLUMN `messages_split_view` TINYINT DEFAULT 0 NOT NULL;
