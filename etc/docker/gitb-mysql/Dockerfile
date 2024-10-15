FROM mysql:8.4.2

ENV MYSQL_ROOT_PASSWORD=root
ENV MYSQL_DATABASE=gitb
ENV MYSQL_USER=gitb
ENV MYSQL_PASSWORD=gitb

COPY gitb-entrypoint.sh /usr/local/bin
COPY gitb.cnf /etc/mysql/conf.d
RUN chmod +x /usr/local/bin/gitb-entrypoint.sh
RUN chmod 644 /etc/mysql/conf.d/gitb.cnf

ENTRYPOINT ["/usr/local/bin/gitb-entrypoint.sh"]
CMD ["mysqld"]