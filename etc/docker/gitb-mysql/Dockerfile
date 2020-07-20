FROM mysql:5.7.31

ENV MYSQL_ROOT_PASSWORD root
ENV MYSQL_DATABASE gitb
ENV MYSQL_USER gitb
ENV MYSQL_PASSWORD gitb

COPY gitb_init.sql /docker-entrypoint-initdb.d
COPY gitb.cnf /etc/mysql/conf.d
