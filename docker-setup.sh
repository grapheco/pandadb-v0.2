docker-compose rm -fs
docker image rm -f pandadb:0.2
mvn package -DskipTests
tar -xzvf packaging/target/pandadb-0.2-unix.tar.gz
docker build -t pandadb:0.2 .
docker-compose up -d