# PandaDB Configuration Settings

This section contains a complete reference of Neo4j configuration settings. They can be set in neo4j.conf.

|  Name | value type | Description |
|---|---|---|
|cn.pandadb.jraft.enabled| Boolean:`true` or `false`| whether use cluster mode or not.|
|cn.pandadb.jraft.server.id| Network Address String, like `localhost:8081` | configure raft endpoint in cluster mode. |
|cn.pandadb.jraft.server.group.id| String |configure raft group name in cluster mode.|
|cn.pandadb.jraft.server.snapshot.enable|Boolean:`true` or `false`|configure using snapshot in cluster mode.|
|cn.pandadb.jraft.server.snapshot.interval.seconds| Integer, like `3600` |configure snapshot interval in cluster mode.|
|cn.pandadb.jraft.server.peers|Network Addresses String,   like `localhost:8081,localhost:8082,localhost:8083`|addresses of all nodes in the cluster|
|costore.factory|class name: `cn.pandadb.costore.InElasticSearchPropertyNodeStoreFactory`|Costore type|
|costore.enable|Boolean:`true` or `false`| whether use ElasticSearch as costore to accelerate node filtering or not.|
|costore.es.host|String,like `localhost` or `127.0.0.1`|hostname of ElasticSearch|
|costore.es.port|Integer, like `9200`|port of ElasticSearch|
|costore.es.schema|`http` or `https`|schema of ElasticSearch|
|costore.es.scroll.size|Integer,like `1000`|scroll size of a scroll query in  ElasticSearch |
|costore.es.scroll.time.minutes|Integer,like `10`| maintain time of scroll query results in ElasticSearch|
|costore.es.index|String|index to use in ElasticSearch|
|costore.es.type|String|type to use in ElasticSearch|
|blob.storage|class name: `org.neo4j.kernel.impl.blob.HBaseBlobValueStorage`|Blob Storage type|
|blob.storage.hbase.zookeeper.quorum|String, like `hbase:2181`|zookeeper quorum of HBase|
|blob.storage.hbase.auto_create_table|Boolean:`true` or `false`|whether auto create table or not|
|blob.storage.hbase.table|String, like `PandadbBlob`|table name to store blob value in HBase|
|aipm.http.host.url|service address like `http://aipm:8081/`|address of AIPM service|
