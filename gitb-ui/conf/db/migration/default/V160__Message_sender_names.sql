-- Snapshot of the sending user's display name at the time the message was sent.
ALTER TABLE `messages` ADD COLUMN `sender_user_name_snapshot` varchar(254) DEFAULT NULL;

-- Community permissions gating the presentation (not the recording) of the sender name snapshot above,
-- for organisation users. Community and Test Bed administrators always see sender names..
ALTER TABLE `communities` ADD COLUMN `allow_admin_sender_names` TINYINT DEFAULT 0 NOT NULL;
ALTER TABLE `communities` ADD COLUMN `allow_organisation_sender_names` TINYINT DEFAULT 0 NOT NULL;

-- Captures, at send time, which kind of organisation the sender/recipient was - a plain organisation,
-- a community's admin organisation, or the Test Bed's own admin organisation.
ALTER TABLE `messages` ADD COLUMN `sender_type` TINYINT NOT NULL DEFAULT 1;
ALTER TABLE `messagerecipients` ADD COLUMN `recipient_type` TINYINT NOT NULL DEFAULT 1;
