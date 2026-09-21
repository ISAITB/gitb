--
-- Extension tables for HTML documentation on specifications and actors.
-- Documentation is optional: presence of a row means documentation is defined; absence means none.
--
-- Live specification documentation.
CREATE TABLE `specificationdocumentation` (
  `id` bigint NOT NULL,
  `documentation` longtext NOT NULL,
  PRIMARY KEY (`id`),
  CONSTRAINT `spec_doc_fk_spec` FOREIGN KEY (`id`) REFERENCES `specifications`(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin;
-- Live actor documentation.
CREATE TABLE `actordocumentation` (
  `id` bigint NOT NULL,
  `documentation` longtext NOT NULL,
  PRIMARY KEY (`id`),
  CONSTRAINT `actor_doc_fk_actor` FOREIGN KEY (`id`) REFERENCES `actors`(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin;
-- Snapshot specification documentation.
-- No FK to conformancesnapshotspecifications because deleted live specs negate the snapshot row ID
-- (to preserve snapshot history), and a FK would block that negation.
CREATE TABLE `conformancesnapshotspecificationdocumentation` (
  `id` bigint NOT NULL,
  `snapshot_id` BIGINT NOT NULL,
  `documentation` longtext NOT NULL,
  PRIMARY KEY (`id`,`snapshot_id`),
  KEY `cs_sp_doc_id` (`id`),
  CONSTRAINT `cs_sp_doc_fk_cs` FOREIGN KEY (`snapshot_id`) REFERENCES `conformancesnapshots`(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin;
-- Snapshot actor documentation.
-- No FK to conformancesnapshotactors for the same negation reason.
CREATE TABLE `conformancesnapshotactordocumentation` (
  `id` bigint NOT NULL,
  `snapshot_id` BIGINT NOT NULL,
  `documentation` longtext NOT NULL,
  PRIMARY KEY (`id`,`snapshot_id`),
  KEY `cs_ac_doc_id` (`id`),
  CONSTRAINT `cs_ac_doc_fk_cs` FOREIGN KEY (`snapshot_id`) REFERENCES `conformancesnapshots`(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin;
