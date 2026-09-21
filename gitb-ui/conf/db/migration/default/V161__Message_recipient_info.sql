-- Snapshot of the message recipient information as send time. Never changes so is precalculated to facilitate lookups.
ALTER TABLE `messages` ADD COLUMN `single_recipient_type` TINYINT DEFAULT NULL;
ALTER TABLE `messages` ADD COLUMN `single_recipient_name_snapshot` varchar(254) DEFAULT NULL;
ALTER TABLE `messages` ADD COLUMN `recipient_count` MEDIUMINT DEFAULT 1;
