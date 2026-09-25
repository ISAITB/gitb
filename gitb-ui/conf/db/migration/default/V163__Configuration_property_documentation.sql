--
-- Extension tables for HTML documentation shown in configuration management forms.
-- Documentation is optional: presence of a row means documentation is defined; absence means none.
-- The id column is a surrogate key (not the owning entity's id) to allow additional documentation
-- purposes to be recorded against the same owner in the future.
--
CREATE TABLE `organisationpropertydocumentation` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `community` bigint NOT NULL,
  `documentation` longtext NOT NULL,
  PRIMARY KEY (`id`),
  KEY `org_prop_doc_community` (`community`),
  CONSTRAINT `org_prop_doc_fk_community` FOREIGN KEY (`community`) REFERENCES `communities`(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin;
CREATE TABLE `systempropertydocumentation` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `community` bigint NOT NULL,
  `documentation` longtext NOT NULL,
  PRIMARY KEY (`id`),
  KEY `sys_prop_doc_community` (`community`),
  CONSTRAINT `sys_prop_doc_fk_community` FOREIGN KEY (`community`) REFERENCES `communities`(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin;
CREATE TABLE `actorpropertydocumentation` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `actor` bigint NOT NULL,
  `documentation` longtext NOT NULL,
  PRIMARY KEY (`id`),
  KEY `actor_prop_doc_actor` (`actor`),
  CONSTRAINT `actor_prop_doc_fk_actor` FOREIGN KEY (`actor`) REFERENCES `actors`(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin;
