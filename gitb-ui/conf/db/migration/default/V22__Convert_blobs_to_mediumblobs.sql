ALTER TABLE `configurations` MODIFY `value` MEDIUMBLOB NOT NULL;
ALTER TABLE `conformancecertificates` MODIFY `keystore_file` MEDIUMBLOB;
ALTER TABLE `domainparameters` MODIFY `value` MEDIUMBLOB NOT NULL;
ALTER TABLE `organisationparametervalues` MODIFY `value` MEDIUMBLOB NOT NULL;
ALTER TABLE `systemparametervalues` MODIFY `value` MEDIUMBLOB NOT NULL;