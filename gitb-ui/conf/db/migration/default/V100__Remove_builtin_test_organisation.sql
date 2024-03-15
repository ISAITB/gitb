-- System
UPDATE `testresults` SET `sut_id` = NULL WHERE `sut_id` IN (SELECT `id` FROM `systems` WHERE `owner` IN (SELECT `id` FROM `organizations` WHERE `id` = 1 AND `community` = 0 AND `admin_organization` = 0));
DELETE FROM `configurations` WHERE `system` IN (SELECT `id` FROM `systems` WHERE `owner` IN (SELECT `id` FROM `organizations` WHERE `id` = 1 AND `community` = 0 AND `admin_organization` = 0));
DELETE FROM `systemhasadmins` WHERE `sut_id` IN (SELECT `id` FROM `systems` WHERE `owner` IN (SELECT `id` FROM `organizations` WHERE `id` = 1 AND `community` = 0 AND `admin_organization` = 0));
DELETE FROM `systemimplementsactors` WHERE `sut_id` IN (SELECT `id` FROM `systems` WHERE `owner` IN (SELECT `id` FROM `organizations` WHERE `id` = 1 AND `community` = 0 AND `admin_organization` = 0));
DELETE FROM `systemimplementsoptions` WHERE `sut_id` IN (SELECT `id` FROM `systems` WHERE `owner` IN (SELECT `id` FROM `organizations` WHERE `id` = 1 AND `community` = 0 AND `admin_organization` = 0));
DELETE FROM `conformanceresults` WHERE `sut_id` IN (SELECT `id` FROM `systems` WHERE `owner` IN (SELECT `id` FROM `organizations` WHERE `id` = 1 AND `community` = 0 AND `admin_organization` = 0));
DELETE FROM `conformancesnapshotresults` WHERE `sut_id` IN (SELECT `id` FROM `systems` WHERE `owner` IN (SELECT `id` FROM `organizations` WHERE `id` = 1 AND `community` = 0 AND `admin_organization` = 0));
DELETE FROM `conformancesnapshotsystems` WHERE `id` IN (SELECT `id` FROM `systems` WHERE `owner` IN (SELECT `id` FROM `organizations` WHERE `id` = 1 AND `community` = 0 AND `admin_organization` = 0));
DELETE FROM `conformancesnapshotsysparams` WHERE `sys_id` IN (SELECT `id` FROM `systems` WHERE `owner` IN (SELECT `id` FROM `organizations` WHERE `id` = 1 AND `community` = 0 AND `admin_organization` = 0));
DELETE FROM `systemparametervalues` WHERE `system` IN (SELECT `id` FROM `systems` WHERE `owner` IN (SELECT `id` FROM `organizations` WHERE `id` = 1 AND `community` = 0 AND `admin_organization` = 0));
DELETE FROM `systems` WHERE `owner` IN (SELECT `id` FROM `organizations` WHERE `id` = 1 AND `community` = 0 AND `admin_organization` = 0);
-- Organisation
UPDATE `testresults` SET `organization_id` = NULL WHERE `organization_id` IN (SELECT `id` FROM `organizations` WHERE `id` = 1 AND `community` = 0 AND `admin_organization` = 0);
DELETE FROM `users` WHERE `organization` IN (SELECT `id` FROM `organizations` WHERE `id` = 1 AND `community` = 0 AND `admin_organization` = 0);
DELETE FROM `organisationparametervalues` WHERE `organisation` IN (SELECT `id` FROM `organizations` WHERE `id` = 1 AND `community` = 0 AND `admin_organization` = 0);
DELETE FROM `conformancesnapshotorganisations` WHERE `id` IN (SELECT `id` FROM `organizations` WHERE `id` = 1 AND `community` = 0 AND `admin_organization` = 0);
DELETE FROM `conformancesnapshotorgparams` WHERE `org_id` IN (SELECT `id` FROM `organizations` WHERE `id` = 1 AND `community` = 0 AND `admin_organization` = 0);
DELETE FROM `organizations` WHERE `id` = 1 AND `community` = 0 AND `admin_organization` = 0;