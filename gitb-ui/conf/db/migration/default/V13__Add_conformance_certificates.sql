CREATE TABLE conformancecertificates (
  id BIGINT PRIMARY KEY NOT NULL AUTO_INCREMENT,
  title VARCHAR(255),
  message LONGTEXT,
  include_message TINYINT DEFAULT 0 NOT NULL,
  include_test_status TINYINT DEFAULT 0 NOT NULL,
  include_test_cases TINYINT DEFAULT 0 NOT NULL,
  include_details TINYINT DEFAULT 0 NOT NULL,
  include_signature TINYINT DEFAULT 0 NOT NULL,
  keystore_file BLOB,
  keystore_type VARCHAR(255),
  keystore_pass VARCHAR(255),
  key_pass VARCHAR(255),
  community BIGINT(20) NOT NULL
);
CREATE UNIQUE INDEX `cs_co_id_idx` ON `conformancecertificates` (`community`);

-- Add default conformance certificate configuration
insert into conformancecertificates(title, include_details, include_message, include_test_status, include_test_cases, include_signature, community)
select
    'Conformance Certificate', 1, 0, 1, 1, 0, communities.id
from communities
left join conformancecertificates on conformancecertificates.community = communities.id
where
    conformancecertificates.id is null;