#!/bin/bash

if [[ -n "$MYSQL_ROOT_PASSWORD_FILE" ]]; then
  unset MYSQL_ROOT_PASSWORD
fi

if [[ -n "$MYSQL_PASSWORD_FILE" ]]; then
  unset MYSQL_PASSWORD
fi

if [[ -z "$MYSQL_ROOT_PASSWORD" && -z "$MYSQL_ROOT_PASSWORD_FILE" ]]; then
  echo "ERROR: No MySQL root password could be determined. Configure MYSQL_ROOT_PASSWORD or MYSQL_ROOT_PASSWORD_FILE." >&2
  exit 1
fi

if [[ -z "$MYSQL_PASSWORD" && -z "$MYSQL_PASSWORD_FILE" ]]; then
  echo "ERROR: No MySQL application password could be determined. Configure MYSQL_PASSWORD or MYSQL_PASSWORD_FILE." >&2
  exit 1
fi

exec /usr/local/bin/docker-entrypoint.sh "$@"