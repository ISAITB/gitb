#!/bin/bash
if [[ -n "$MYSQL_ROOT_PASSWORD_FILE" ]] ; then
  unset MYSQL_ROOT_PASSWORD
fi
if [[ -n "$MYSQL_PASSWORD_FILE" ]] ; then
  unset MYSQL_PASSWORD
fi
exec /usr/local/bin/docker-entrypoint.sh "$@"