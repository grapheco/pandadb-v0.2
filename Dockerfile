FROM nimmis/java-centos:oracle-8-jdk
WORKDIR /usr/share/pandadb
COPY pandadb-0.2 .
RUN bash /usr/share/pandadb/bin/pandadb.sh start