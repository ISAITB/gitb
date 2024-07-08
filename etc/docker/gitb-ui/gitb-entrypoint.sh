#!/bin/bash

cat > /usr/local/gitb-ui/conf/overrides.conf << EOF
include "application"
EOF

if [[ -n "$DB_DEFAULT_PASSWORD_FILE" ]] ; then 
    echo "slick.dbs.default.db.password=$(cat $DB_DEFAULT_PASSWORD_FILE)" >> /usr/local/gitb-ui/conf/overrides.conf;
    echo "db.default.password=$(cat $DB_DEFAULT_PASSWORD_FILE)" >> /usr/local/gitb-ui/conf/overrides.conf;
fi
if [[ -n "$APPLICATION_SECRET_FILE" ]] ; then 
    echo "play.http.secret.key=$(cat $APPLICATION_SECRET_FILE)" >> /usr/local/gitb-ui/conf/overrides.conf;
fi
if [[ -n "$MASTER_PASSWORD_FILE" ]] ; then 
    echo "masterPassword=$(cat $MASTER_PASSWORD_FILE)" >> /usr/local/gitb-ui/conf/overrides.conf;
fi
if [[ -n "$HMAC_KEY_FILE" ]] ; then 
    echo "hmac.key=$(cat $HMAC_KEY_FILE)" >> /usr/local/gitb-ui/conf/overrides.conf;
fi
if [[ -n "$DATA_ARCHIVE_KEY_FILE" ]] ; then 
    echo "dataArchive.key=$(cat $DATA_ARCHIVE_KEY_FILE)" >> /usr/local/gitb-ui/conf/overrides.conf;
fi
if [[ -n "$PROXY_SERVER_AUTH_PASSWORD_FILE" ]] ; then 
    echo "proxy.auth.password=$(cat $PROXY_SERVER_AUTH_PASSWORD_FILE)" >> /usr/local/gitb-ui/conf/overrides.conf;
fi
if [[ -n "$EMAIL_SMTP_AUTH_PASSWORD_FILE" ]] ; then 
    echo "email.smtp.auth.password=$(cat $EMAIL_SMTP_AUTH_PASSWORD_FILE)" >> /usr/local/gitb-ui/conf/overrides.conf;
fi

exec gitb -Dconfig.file=/usr/local/gitb-ui/conf/overrides.conf