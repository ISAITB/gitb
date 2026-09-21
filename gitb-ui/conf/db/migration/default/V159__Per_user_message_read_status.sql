-- Read status moves from being tracked per recipient organisation (MessageRecipients.read_at) to being
-- tracked per user. A row here means the message is unread for the given user; a missing row means it is
-- read (or was never delivered to that user, e.g. an account created after the message was sent).
CREATE TABLE `messageunreadstatus` (
  `recipient_id` BIGINT NOT NULL,
  `user_id` BIGINT NOT NULL,
  PRIMARY KEY (`recipient_id`, `user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX `msgunread_user_idx` ON `messageunreadstatus` (`user_id`);

ALTER TABLE `messageunreadstatus` ADD CONSTRAINT `msgunread_fk_recipient` FOREIGN KEY (`recipient_id`) REFERENCES `messagerecipients`(`id`);
ALTER TABLE `messageunreadstatus` ADD CONSTRAINT `msgunread_fk_user` FOREIGN KEY (`user_id`) REFERENCES `users`(`id`);

-- This feature is still in developer preview, so no data migration is needed - all existing delivered
-- messages are simply treated as read for everyone (no rows added here).
ALTER TABLE `messagerecipients` DROP COLUMN `read_at`;
