-- Specification group table.
CREATE TABLE `specificationgroups` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `sname` varchar(254) NOT NULL,
  `fname` varchar(254) NOT NULL,
  `description` text,
  `domain` BIGINT NOT NULL,
  PRIMARY KEY (`id`)
);
-- Association with domains
CREATE INDEX `spec_idx_domain` on `specificationgroups`(`domain`);
ALTER TABLE `specificationgroups` ADD CONSTRAINT `spec_group_fk_domain` FOREIGN KEY (`domain`) REFERENCES `domains`(`id`);
-- Association with specifications
ALTER TABLE `specifications` ADD COLUMN `spec_group` BIGINT;
CREATE INDEX `spec_idx_spec_group` on `specifications`(`spec_group`);
ALTER TABLE `specifications` ADD CONSTRAINT `spec_fk_spec_group` FOREIGN KEY (`spec_group`) REFERENCES `specificationgroups`(`id`);