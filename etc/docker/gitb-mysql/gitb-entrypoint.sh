#!/bin/bash

resolve_env_fallback() {
  local base_name="$1"
  local file_var_name="${base_name}_FILE"
  local value_var_name="${base_name}"
  local dev_file_var_name="${base_name}_FILE_DEV"
  local default_dev_file="/etc/mysql/extra-configs/${base_name}"

  if [[ -z "${!file_var_name}" && -z "${!value_var_name}" ]]; then
    local dev_file_path="${!dev_file_var_name}"
    if [[ -z "$dev_file_path" ]]; then
      dev_file_path="$default_dev_file"
    fi
    if [[ -f "$dev_file_path" ]]; then
      local secret_value
      secret_value="$(<"$dev_file_path")"
      export "$value_var_name=$secret_value"
    fi
  fi
}

resolve_env_fallback "MYSQL_ROOT_PASSWORD"
resolve_env_fallback "MYSQL_PASSWORD"

if [[ -n "$MYSQL_ROOT_PASSWORD_FILE" ]] ; then
  unset MYSQL_ROOT_PASSWORD
fi
if [[ -n "$MYSQL_PASSWORD_FILE" ]] ; then
  unset MYSQL_PASSWORD
fi
exec /usr/local/bin/docker-entrypoint.sh "$@"