FROM mysql:8.4.8

ENV MYSQL_DATABASE=gitb
ENV MYSQL_USER=gitb

COPY gitb-entrypoint.sh /usr/local/bin
COPY gitb.cnf /etc/mysql/conf.d
COPY extra-configs /etc/mysql/extra-configs
RUN chmod +x /usr/local/bin/gitb-entrypoint.sh
RUN chmod 644 /etc/mysql/conf.d/gitb.cnf

ENTRYPOINT ["/usr/local/bin/gitb-entrypoint.sh"]
CMD ["mysqld"]