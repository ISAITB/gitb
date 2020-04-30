-- Communities
UPDATE `communities` SET `domain` = NULL WHERE `domain` IS NOT NULL AND `domain` NOT IN (SELECT `id` FROM `domains`);
ALTER TABLE `communities` ADD CONSTRAINT `com_fk_dom` FOREIGN KEY (`domain`) REFERENCES `domains`(`id`);
-- Error templates
DELETE FROM `errortemplates` WHERE `community` NOT IN (SELECT `id` FROM `communities`);
UPDATE `organizations` SET `error_template` = NULL WHERE `error_template` IS NOT NULL AND `error_template` NOT IN (SELECT `id` from `errortemplates`);
ALTER TABLE `errortemplates` ADD CONSTRAINT `errtem_fk_com` FOREIGN KEY (`community`) REFERENCES `communities`(`id`);
-- Landing pages
DELETE FROM `landingpages` WHERE `community` NOT IN (SELECT `id` FROM `communities`);
UPDATE `organizations` SET `landing_page` = NULL WHERE `landing_page` IS NOT NULL AND `landing_page` NOT IN (SELECT `id` from `landingpages`);
ALTER TABLE `landingpages` ADD CONSTRAINT `lanpag_fk_com` FOREIGN KEY (`community`) REFERENCES `communities`(`id`);
-- Legal notices
DELETE FROM `legalnotices` WHERE `community` NOT IN (SELECT `id` FROM `communities`);
UPDATE `organizations` SET `legal_notice` = NULL WHERE `legal_notice` IS NOT NULL AND `legal_notice` NOT IN (SELECT `id` from `legalnotices`);
ALTER TABLE `legalnotices` ADD CONSTRAINT `legnot_fk_com` FOREIGN KEY (`community`) REFERENCES `communities`(`id`);
-- Conformance certificates
DELETE FROM `conformancecertificates` WHERE `community` NOT IN (SELECT `id` FROM `communities`);
ALTER TABLE `conformancecertificates` ADD CONSTRAINT `ccert_fk_com` FOREIGN KEY (`community`) REFERENCES `communities`(`id`);
-- Organisations
DELETE FROM `organizations` WHERE `community` NOT IN (SELECT `id` FROM `communities`);
ALTER TABLE `organizations` ADD CONSTRAINT `org_fk_com` FOREIGN KEY (`community`) REFERENCES `communities`(`id`);
ALTER TABLE `organizations` ADD CONSTRAINT `org_fk_lanpag` FOREIGN KEY (`landing_page`) REFERENCES `landingpages`(`id`);
ALTER TABLE `organizations` ADD CONSTRAINT `org_fk_legnot` FOREIGN KEY (`legal_notice`) REFERENCES `legalnotices`(`id`);
ALTER TABLE `organizations` ADD CONSTRAINT `org_fk_errtem` FOREIGN KEY (`error_template`) REFERENCES `errortemplates`(`id`);
-- Users
DELETE FROM `users` WHERE `organization` NOT IN (SELECT `id` FROM `organizations`);
ALTER TABLE `users` ADD CONSTRAINT `use_fk_org` FOREIGN KEY (`organization`) REFERENCES `organizations`(`id`);
-- Systems
DELETE FROM `systems` WHERE `owner` NOT IN (SELECT `id` FROM `organizations`);
ALTER TABLE `systems` ADD CONSTRAINT `sut_fk_org` FOREIGN KEY (`owner`) REFERENCES `organizations`(`id`);
-- System has admins
DELETE FROM `systemhasadmins` WHERE `sut_id` NOT IN (SELECT `id` FROM `systems`);
ALTER TABLE `systemhasadmins` ADD CONSTRAINT `suthadm_fk_sut` FOREIGN KEY (`sut_id`) REFERENCES `systems`(`id`);
DELETE FROM `systemhasadmins` WHERE `user_id` NOT IN (SELECT `id` FROM `users`);
ALTER TABLE `systemhasadmins` ADD CONSTRAINT `suthadm_fk_use` FOREIGN KEY (`user_id`) REFERENCES `users`(`id`);
-- Transactions
DELETE FROM `transactions` WHERE `domain` NOT IN (SELECT `id` FROM `domains`);
ALTER TABLE `transactions` ADD CONSTRAINT `tra_fk_dom` FOREIGN KEY (`domain`) REFERENCES `domains`(`id`);
-- Specifications
DELETE FROM `specifications` WHERE `domain` NOT IN (SELECT `id` FROM `domains`);
ALTER TABLE `specifications` ADD CONSTRAINT `spe_fk_dom` FOREIGN KEY (`domain`) REFERENCES `domains`(`id`);
-- Actors
DELETE FROM `actors` WHERE `domain` NOT IN (SELECT `id` FROM `domains`);
ALTER TABLE `actors` ADD CONSTRAINT `ac_fk_dom` FOREIGN KEY (`domain`) REFERENCES `domains`(`id`);
-- Specification has actors
DELETE FROM `specificationhasactors` WHERE `spec_id` NOT IN (SELECT `id` FROM `specifications`);
DELETE FROM `specificationhasactors` WHERE `actor_id` NOT IN (SELECT `id` FROM `actors`);
ALTER TABLE `specificationhasactors` ADD CONSTRAINT `spehact_fk_spe` FOREIGN KEY (`spec_id`) REFERENCES `specifications`(`id`);
ALTER TABLE `specificationhasactors` ADD CONSTRAINT `spehact_fk_act` FOREIGN KEY (`actor_id`) REFERENCES `actors`(`id`);
-- Endpoints
DELETE FROM `endpoints` WHERE `actor` NOT IN (SELECT `id` FROM `actors`);
ALTER TABLE `endpoints` ADD CONSTRAINT `end_fk_act` FOREIGN KEY (`actor`) REFERENCES `actors`(`id`);
-- Endpoint supports transactions
DELETE FROM `endpointsupportstransactions` WHERE `actor` NOT IN (SELECT `id` FROM `actors`);
ALTER TABLE `endpointsupportstransactions` ADD CONSTRAINT `endsuptra_fk_act` FOREIGN KEY (`actor`) REFERENCES `actors`(`id`);
-- Options
DELETE FROM `options` WHERE `actor` NOT IN (SELECT `id` FROM `actors`);
ALTER TABLE `options` ADD CONSTRAINT `opt_fk_act` FOREIGN KEY (`actor`) REFERENCES `actors`(`id`);
-- Test suites
DELETE FROM `testsuites` WHERE `specification` NOT IN (SELECT `id` FROM `specifications`);
ALTER TABLE `testsuites` ADD CONSTRAINT `tessui_fk_spe` FOREIGN KEY (`specification`) REFERENCES `specifications`(`id`);
-- System implements actors
DELETE FROM `systemimplementsactors` WHERE `sut_id` NOT IN (SELECT `id` FROM `systems`);
DELETE FROM `systemimplementsactors` WHERE `spec_id` NOT IN (SELECT `id` FROM `specifications`);
DELETE FROM `systemimplementsactors` WHERE `actor_id` NOT IN (SELECT `id` FROM `actors`);
ALTER TABLE `systemimplementsactors` ADD CONSTRAINT `sutiact_fk_sut` FOREIGN KEY (`sut_id`) REFERENCES `systems`(`id`);
ALTER TABLE `systemimplementsactors` ADD CONSTRAINT `sutiact_fk_spe` FOREIGN KEY (`spec_id`) REFERENCES `specifications`(`id`);
ALTER TABLE `systemimplementsactors` ADD CONSTRAINT `sutiact_fk_act` FOREIGN KEY (`actor_id`) REFERENCES `actors`(`id`);
-- System implements options
DELETE FROM `systemimplementsoptions` WHERE `sut_id` NOT IN (SELECT `id` FROM `systems`);
DELETE FROM `systemimplementsoptions` WHERE `option_id` NOT IN (SELECT `id` FROM `options`);
ALTER TABLE `systemimplementsoptions` ADD CONSTRAINT `sutiopt_fk_sut` FOREIGN KEY (`sut_id`) REFERENCES `systems`(`id`);
ALTER TABLE `systemimplementsoptions` ADD CONSTRAINT `sutiopt_fk_opt` FOREIGN KEY (`option_id`) REFERENCES `options`(`id`);
-- Test cases
DELETE FROM `testcases` WHERE `target_spec` NOT IN (SELECT `id` FROM `specifications`);
ALTER TABLE `testcases` ADD CONSTRAINT `tescas_fk_spe` FOREIGN KEY (`target_spec`) REFERENCES `specifications`(`id`);
-- Test suite has test cases
DELETE FROM `testsuitehastestcases` WHERE `testsuite` NOT IN (SELECT `id` FROM `testsuites`);
DELETE FROM `testsuitehastestcases` WHERE `testcase` NOT IN (SELECT `id` FROM `testcases`);
ALTER TABLE `testsuitehastestcases` ADD CONSTRAINT `tessuihtescas_fk_tessui` FOREIGN KEY (`testsuite`) REFERENCES `testsuites`(`id`);
ALTER TABLE `testsuitehastestcases` ADD CONSTRAINT `tessuihtescas_fk_tescas` FOREIGN KEY (`testcase`) REFERENCES `testcases`(`id`);
-- Test case covers options
DELETE FROM `testcasecoversoptions` WHERE `testcase` NOT IN (SELECT `id` FROM `testcases`);
DELETE FROM `testcasecoversoptions` WHERE `option` NOT IN (SELECT `id` FROM `options`);
ALTER TABLE `testcasecoversoptions` ADD CONSTRAINT `tescovopt_fk_tes` FOREIGN KEY (`testcase`) REFERENCES `testcases`(`id`);
ALTER TABLE `testcasecoversoptions` ADD CONSTRAINT `tescovopt_fk_opt` FOREIGN KEY (`option`) REFERENCES `options`(`id`);
-- Test case has actors
DELETE FROM `testcasehasactors` WHERE `testcase` NOT IN (SELECT `id` FROM `testcases`);
DELETE FROM `testcasehasactors` WHERE `specification` NOT IN (SELECT `id` FROM `specifications`);
DELETE FROM `testcasehasactors` WHERE `actor` NOT IN (SELECT `id` FROM `actors`);
ALTER TABLE `testcasehasactors` ADD CONSTRAINT `teshact_fk_tes` FOREIGN KEY (`testcase`) REFERENCES `testcases`(`id`);
ALTER TABLE `testcasehasactors` ADD CONSTRAINT `teshact_fk_spe` FOREIGN KEY (`specification`) REFERENCES `specifications`(`id`);
ALTER TABLE `testcasehasactors` ADD CONSTRAINT `teshact_fk_act` FOREIGN KEY (`actor`) REFERENCES `actors`(`id`);
-- Test suite has actors
DELETE FROM `testsuitehasactors` WHERE `testsuite` NOT IN (SELECT `id` FROM `testsuites`);
DELETE FROM `testsuitehasactors` WHERE `actor` NOT IN (SELECT `id` FROM `actors`);
ALTER TABLE `testsuitehasactors` ADD CONSTRAINT `tessuihact_fk_tessui` FOREIGN KEY (`testsuite`) REFERENCES `testsuites`(`id`);
ALTER TABLE `testsuitehasactors` ADD CONSTRAINT `tessuihact_fk_act` FOREIGN KEY (`actor`) REFERENCES `actors`(`id`);