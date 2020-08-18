FROM openjdk:8
WORKDIR /usr/share/pandadb
COPY pandadb-0.2 .
RUN bash /usr/share/pandadb/bin/pandadb.sh start