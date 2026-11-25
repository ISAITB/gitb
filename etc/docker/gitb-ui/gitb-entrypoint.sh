#!/bin/bash

cat > /usr/local/gitb-ui/conf/overrides.conf << EOF
include "application"
EOF

resolve_secret_value() {
    local base_name="$1"
    local file_var_name="${base_name}_FILE"
    local dev_file_var_name="${base_name}_FILE_DEV"
    local default_dev_file="/usr/local/gitb-ui/conf/extra-configs/${base_name}"

    if [[ -n "${!file_var_name}" ]] ; then
        cat "${!file_var_name}"
    elif [[ -n "${!base_name}" ]] ; then
        printf '%s' "${!base_name}"
    else
        local dev_file_path="${!dev_file_var_name}"
        if [[ -z "$dev_file_path" ]] ; then
            dev_file_path="$default_dev_file"
        fi
        if [[ -f "$dev_file_path" ]] ; then
            cat "$dev_file_path"
        fi
    fi
}

append_override() {
    local key="$1"
    local value="$2"
    if [[ -n "$value" ]] ; then
        echo -n "${key}=\"\"\"" >> /usr/local/gitb-ui/conf/overrides.conf
        echo -n "$value" >> /usr/local/gitb-ui/conf/overrides.conf
        echo '"""' >> /usr/local/gitb-ui/conf/overrides.conf
    fi
}

dbPassword="$(resolve_secret_value "DB_DEFAULT_PASSWORD")"
append_override "slick.dbs.default.db.password" "$dbPassword"
append_override "db.default.password" "$dbPassword"
append_override "play.http.secret.key" "$(resolve_secret_value "APPLICATION_SECRET")"
append_override "masterPassword" "$(resolve_secret_value "MASTER_PASSWORD")"
append_override "masterApiKey" "$(resolve_secret_value "AUTOMATION_API_MASTER_KEY")"
append_override "hmac.key" "$(resolve_secret_value "HMAC_KEY")"
append_override "dataArchive.key" "$(resolve_secret_value "DATA_ARCHIVE_KEY")"
append_override "proxy.auth.password" "$(resolve_secret_value "PROXY_SERVER_AUTH_PASSWORD")"
append_override "email.smtp.auth.password" "$(resolve_secret_value "EMAIL_SMTP_AUTH_PASSWORD")"
append_override "authentication.sso.clientSecret" "$(resolve_secret_value "AUTHENTICATION_SSO_CLIENT_SECRET")"
append_override "authentication.sso.connectionPassword" "$(resolve_secret_value "AUTHENTICATION_SSO_CONNECTION_PASSWORD")"

exec gitb -Dconfig.file=/usr/local/gitb-ui/conf/overrides.conf