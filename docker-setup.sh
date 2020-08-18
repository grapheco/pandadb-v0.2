mvn package -DskipTests
cd packaging/target/
tar -xzvf pandadb-0.2-unix.tar.gz
docker build -t pandadb:0.2 .
docker-compose up -d