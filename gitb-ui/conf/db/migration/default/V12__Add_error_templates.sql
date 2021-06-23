CREATE TABLE errortemplates (
  id BIGINT PRIMARY KEY NOT NULL AUTO_INCREMENT,
  name VARCHAR(255) NOT NULL,
  content LONGTEXT,
  default_flag TINYINT DEFAULT 0 NOT NULL,
  description LONGTEXT,
  community BIGINT NOT NULL
);
ALTER TABLE organizations ADD error_template BIGINT NULL;

