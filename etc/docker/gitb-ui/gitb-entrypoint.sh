#!/bin/bash

cat > /usr/local/gitb-ui/conf/overrides.conf << EOF
include "application"
EOF

if [[ -n "$DB_DEFAULT_PASSWORD_FILE" ]] ; then
    echo -n 'slick.dbs.default.db.password="""' >> /usr/local/gitb-ui/conf/overrides.conf;
    echo -n $(cat $DB_DEFAULT_PASSWORD_FILE) >> /usr/local/gitb-ui/conf/overrides.conf;
    echo '"""' >> /usr/local/gitb-ui/conf/overrides.conf;
    echo -n 'db.default.password="""' >> /usr/local/gitb-ui/conf/overrides.conf;
    echo -n $(cat $DB_DEFAULT_PASSWORD_FILE) >> /usr/local/gitb-ui/conf/overrides.conf;
    echo '"""' >> /usr/local/gitb-ui/conf/overrides.conf;
fi
if [[ -n "$APPLICATION_SECRET_FILE" ]] ; then
    echo -n 'play.http.secret.key="""' >> /usr/local/gitb-ui/conf/overrides.conf;
    echo -n $(cat $APPLICATION_SECRET_FILE) >> /usr/local/gitb-ui/conf/overrides.conf;
    echo '"""' >> /usr/local/gitb-ui/conf/overrides.conf;
fi
if [[ -n "$MASTER_PASSWORD_FILE" ]] ; then
    echo -n 'masterPassword="""' >> /usr/local/gitb-ui/conf/overrides.conf;
    echo -n $(cat $MASTER_PASSWORD_FILE) >> /usr/local/gitb-ui/conf/overrides.conf;
    echo '"""' >> /usr/local/gitb-ui/conf/overrides.conf;
fi
if [[ -n "$AUTOMATION_API_MASTER_KEY_FILE" ]] ; then
    echo -n 'masterApiKey="""' >> /usr/local/gitb-ui/conf/overrides.conf;
    echo -n $(cat $AUTOMATION_API_MASTER_KEY_FILE) >> /usr/local/gitb-ui/conf/overrides.conf;
    echo '"""' >> /usr/local/gitb-ui/conf/overrides.conf;
fi
if [[ -n "$HMAC_KEY_FILE" ]] ; then
    echo -n 'hmac.key="""' >> /usr/local/gitb-ui/conf/overrides.conf;
    echo -n $(cat $HMAC_KEY_FILE) >> /usr/local/gitb-ui/conf/overrides.conf;
    echo '"""' >> /usr/local/gitb-ui/conf/overrides.conf;
fi
if [[ -n "$DATA_ARCHIVE_KEY_FILE" ]] ; then
    echo -n 'dataArchive.key="""' >> /usr/local/gitb-ui/conf/overrides.conf;
    echo -n $(cat $DATA_ARCHIVE_KEY_FILE) >> /usr/local/gitb-ui/conf/overrides.conf;
    echo '"""' >> /usr/local/gitb-ui/conf/overrides.conf;
fi
if [[ -n "$PROXY_SERVER_AUTH_PASSWORD_FILE" ]] ; then
    echo -n 'proxy.auth.password="""' >> /usr/local/gitb-ui/conf/overrides.conf;
    echo -n $(cat $PROXY_SERVER_AUTH_PASSWORD_FILE) >> /usr/local/gitb-ui/conf/overrides.conf;
    echo '"""' >> /usr/local/gitb-ui/conf/overrides.conf;
fi
if [[ -n "$EMAIL_SMTP_AUTH_PASSWORD_FILE" ]] ; then
    echo -n 'email.smtp.auth.password="""' >> /usr/local/gitb-ui/conf/overrides.conf;
    echo -n $(cat $EMAIL_SMTP_AUTH_PASSWORD_FILE) >> /usr/local/gitb-ui/conf/overrides.conf;
    echo '"""' >> /usr/local/gitb-ui/conf/overrides.conf;
fi
if [[ -n "$AUTHENTICATION_SSO_CLIENT_SECRET_FILE" ]] ; then
    echo -n 'authentication.sso.clientSecret="""' >> /usr/local/gitb-ui/conf/overrides.conf;
    echo -n $(cat $AUTHENTICATION_SSO_CLIENT_SECRET_FILE) >> /usr/local/gitb-ui/conf/overrides.conf;
    echo '"""' >> /usr/local/gitb-ui/conf/overrides.conf;
fi

exec gitb -Dconfig.file=/usr/local/gitb-ui/conf/overrides.conf