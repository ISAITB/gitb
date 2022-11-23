CREATE TABLE `communityresources` (
  `id` BIGINT PRIMARY KEY NOT NULL AUTO_INCREMENT,
  `name` varchar(254) NOT NULL,
  `description` text,
  `community` BIGINT NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
ALTER TABLE `communityresources` ADD CONSTRAINT `cr_fk_com` FOREIGN KEY (`community`) REFERENCES `communities`(`id`);
CREATE UNIQUE INDEX `cr_com_name_idx` ON `communityresources` (`name`,`community`);