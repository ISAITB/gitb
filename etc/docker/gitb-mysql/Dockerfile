FROM mysql:8.4.8

# Passwords set at build-time or overriden by user configuration.
# Passwords can also be set via secret files in which case the
# entrypoint removes these variables.
# The entrypoint script also fails if passwords are not set.
ARG MYSQL_ROOT_PASSWORD=""
ARG MYSQL_PASSWORD=""

ENV MYSQL_ROOT_PASSWORD=${MYSQL_ROOT_PASSWORD}
ENV MYSQL_DATABASE=gitb
ENV MYSQL_USER=gitb
ENV MYSQL_PASSWORD=${MYSQL_PASSWORD}

COPY gitb-entrypoint.sh /usr/local/bin
COPY gitb.cnf /etc/mysql/conf.d
RUN chmod +x /usr/local/bin/gitb-entrypoint.sh
RUN chmod 644 /etc/mysql/conf.d/gitb.cnf

ENTRYPOINT ["/usr/local/bin/gitb-entrypoint.sh"]
CMD ["mysqld"]