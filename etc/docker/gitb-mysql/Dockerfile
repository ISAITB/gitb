FROM mysql:8.4.0

ENV MYSQL_ROOT_PASSWORD root
ENV MYSQL_DATABASE gitb
ENV MYSQL_USER gitb
ENV MYSQL_PASSWORD gitb

COPY gitb.cnf /etc/mysql/conf.d
RUN chmod 644 /etc/mysql/conf.d/gitb.cnf