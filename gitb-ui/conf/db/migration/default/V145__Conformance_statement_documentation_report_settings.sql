--
-- Create conformance statement documentation report settings table.
--
CREATE TABLE `conformancestatementdocumentationreportsettings` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `enabled` tinyint NOT NULL DEFAULT '0',
  `include_overview` tinyint NOT NULL DEFAULT '1',
  `include_statement_documentation` tinyint NOT NULL DEFAULT '1',
  `include_test_case_listing` tinyint NOT NULL DEFAULT '1',
  `include_test_suite_documentation` tinyint NOT NULL DEFAULT '1',
  `include_test_case_documentation` tinyint NOT NULL DEFAULT '1',
  `include_signature` tinyint NOT NULL DEFAULT '0',
  `community` bigint NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `csdrs_com_idx` (`community`),
  CONSTRAINT `csdrs_fk_com` FOREIGN KEY (`community`) REFERENCES `communities` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin;
