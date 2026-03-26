-- User preferences
CREATE TABLE `userpreferences` (
 `id` BIGINT NOT NULL AUTO_INCREMENT,
 `menu_collapsed` TINYINT DEFAULT 1 NOT NULL,
 `statements_collapsed` TINYINT DEFAULT 0 NOT NULL,
 `page_size` SMALLINT DEFAULT 10 NOT NULL,
 `user` BIGINT NOT NULL,
PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
ALTER TABLE `userpreferences` ADD CONSTRAINT `userpref_user_fk` FOREIGN KEY (`user`) REFERENCES `users`(`id`);
CREATE UNIQUE INDEX `userpref_idx_user` ON `userpreferences`(`user`);
-- Community default user preferences
CREATE TABLE `userpreferencedefaults` (
                                   `id` BIGINT NOT NULL AUTO_INCREMENT,
                                   `menu_collapsed` TINYINT DEFAULT 1 NOT NULL,
                                   `statements_collapsed` TINYINT DEFAULT 0 NOT NULL,
                                   `page_size` SMALLINT DEFAULT 10 NOT NULL,
                                   `community` BIGINT NOT NULL,
                                   PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
ALTER TABLE `userpreferencedefaults` ADD CONSTRAINT `userprefdef_com_fk` FOREIGN KEY (`community`) REFERENCES `communities`(`id`);
CREATE UNIQUE INDEX `userprefdef_idx_com` ON `userpreferencedefaults`(`community`);
-- Migration
INSERT INTO `userpreferences`(`user`) SELECT `id` FROM `users`;
INSERT INTO `userpreferencedefaults`(`community`) SELECT `id` FROM `communities`;
