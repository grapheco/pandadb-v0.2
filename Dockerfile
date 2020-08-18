FROM nimmis/java-centos:oracle-8-jdk
WORKDIR /usr/share/pandadb
COPY packaging/target/pandadb-0.2 .
RUN bash /usr/share/pandadb-0.2/bin/pandadb.sh start