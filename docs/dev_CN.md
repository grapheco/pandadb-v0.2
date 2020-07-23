# 开发说明文档

## 1. 模块功能说明

### 业务模块

- pandadb-raft： 负责提供raft集群服务，包括：Leader选举，将Leader节点的图库操作事务日志同步到follower节点；
- neo4j-hacking：负责对neo4j源码的改动。

### 测试模块

- pandadb-raft-itest：负责维护pandadb-raft模块功能的测试代码。



## 2. 模块依赖说明

neo4j/neo4j-server  <-  neo4j-hacking  <-  pandadb-raft



 pandadb-raft <- pandadb-raft-itest