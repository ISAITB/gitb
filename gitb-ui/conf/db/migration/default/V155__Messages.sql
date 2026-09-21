CREATE TABLE `messages` (
  `id` BIGINT PRIMARY KEY NOT NULL AUTO_INCREMENT,
  `subject` varchar(512),
  `body` text,
  `body_text` text,
  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `deleted_by_sender_at` TIMESTAMP NULL DEFAULT NULL,
  `parent_message_id` BIGINT,
  `thread_id` BIGINT NOT NULL,
  `sender_id` BIGINT,
  `sender_name_snapshot` varchar(254) NOT NULL,
  `sender_user_id` BIGINT,
  `important` tinyint NOT NULL DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX `msg_parent_idx` ON `messages` (`parent_message_id`);
CREATE INDEX `msg_thread_idx` ON `messages` (`thread_id`);
CREATE INDEX `msg_sender_idx` ON `messages` (`sender_id`);
CREATE INDEX `msg_outbox_idx` ON `messages` (`sender_id`, `created_at` DESC);

ALTER TABLE `messages` ADD CONSTRAINT `msg_fk_parent` FOREIGN KEY (`parent_message_id`) REFERENCES `messages`(`id`);
ALTER TABLE `messages` ADD CONSTRAINT `msg_fk_sender` FOREIGN KEY (`sender_id`) REFERENCES `organizations`(`id`);
ALTER TABLE `messages` ADD CONSTRAINT `msg_fk_sender_user` FOREIGN KEY (`sender_user_id`) REFERENCES `users`(`id`);

CREATE TABLE `messagerecipients` (
  `id` BIGINT PRIMARY KEY NOT NULL AUTO_INCREMENT,
  `message_id` BIGINT NOT NULL,
  `recipient_id` BIGINT,
  `recipient_name_snapshot` varchar(254) NOT NULL,
  `delivered_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `read_at` TIMESTAMP NULL DEFAULT NULL,
  `deleted_by_recipient_at` TIMESTAMP NULL DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX `msgrec_message_idx` ON `messagerecipients` (`message_id`);
CREATE INDEX `msgrec_recipient_idx` ON `messagerecipients` (`recipient_id`);
CREATE UNIQUE INDEX `msgrec_unique_idx` ON `messagerecipients` (`message_id`, `recipient_id`);
CREATE INDEX `msgrec_inbox_idx` ON `messagerecipients` (`recipient_id`, `deleted_by_recipient_at`, `delivered_at` DESC);

ALTER TABLE `messagerecipients` ADD CONSTRAINT `msgrec_fk_message` FOREIGN KEY (`message_id`) REFERENCES `messages`(`id`);
ALTER TABLE `messagerecipients` ADD CONSTRAINT `msgrec_fk_recipient` FOREIGN KEY (`recipient_id`) REFERENCES `organizations`(`id`);
