mvn clean package -DskipTests
docker build -t pandadb:0.2 .
docker-compose up -d