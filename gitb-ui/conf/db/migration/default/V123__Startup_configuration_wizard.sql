-- In the case of migrations, force the startup wizard to be inactive.
INSERT INTO `systemconfigurations`(name, parameter) VALUES ('startup_wizard', 'false');
