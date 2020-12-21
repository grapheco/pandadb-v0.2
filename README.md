# PandaDB v0.2
intelligent graph database

* intelligent property graph management
* distributed graph
* ElasticSearch as costore backend
* HBase as BLOB storage backend

# Licensing
PandaDB v0.2 is an open source product licensed under GPLv3.

# Limitation

* Unsupport cypher point type, list type and duration type.



## 1. Building PandaDB

### 1.1. install all artifacts

```
mvn clean install
```

### 1.2. building server-side distribution zip package

```
cd packaging
mvn package -Pserver-unix-dist
```

or

```
cd packaging
mvn package -Pserver-win-dist
```

this command will create `pandadb-server-<version>-unix.tgz` or `pandadb-server-<version>-win.zip` in `target` directory.

### 1.3. building server-side all-in-one jar package

```
cd packaging
mvn package -Pserver-jar
```

this command will create `pandadb-server-all-in-one-<version>.jar` in `target` directory.



## 2. Quick start

### 2.1 Deploy ElasticSearch 、 HBase and AIPM Service

- Install ElasticSearch :

  please visit [ElasticSearch Guid Page](https://www.elastic.co/guide/index.html) to install ElasticSearch.

- Install HBase :

  please visit [HBASE Home Page](http://hbase.apache.org/) to install HBase.

- install AIPM

​      please visit https://github.com/cas-bigdatalab/aipm-web to deploy aipm service.



### 2.2 start single node PandaDB

#### (1) Download package

visit https://github.com/grapheco/pandadb-v0.2/releases to get pandadb-v0.2 binary distributions.

unpack `pandadb-server-<version>-unix.tar.gz` in your local directory, e.g. `/usr/local/`.

#### (2) Modify the configuration file

```
cd /usr/local/pandadb-server-<version>
vi conf/neo4j.conf
```

- modify costore related configurations refer to the following example:

 ```
  costore.enable=true
  # replace <es-host> and <es-port> with actual hostname and port
  costore.es.host=<es-host>
  costore.es.port=<es-port>
  costore.es.index=pandadb-costore
  costore.es.type=nodes
 ```

- modify HBase Blob Storage related configurations refer to the following example:

 ```
  # replace <zk-host:port> with actual hbase zookeeper quorum
  blob.storage.hbase.zookeeper.quorum=<zk-host:port>
  blob.storage.hbase.auto_create_table=true
  blob.storage.hbase.table=PANDADB_BLOB
 ```

 - modify AIPM configurations refer to the following example:

 ```
   # replace <aipm-url> with actual AIPM URL
   aipm.http.host.url=<aipm-url>
 ```

#### (3) start

- modify configuration file to unenable `cn.pandadb.jraft.enabled`

```
  cn.pandadb.jraft.enabled=false
```

- start PandaDB server

```
  # start a PandaDB server silently
  cd /usr/local/pandadb-server-<version>
  bin/neo4j start
```



### 2.3 start multi-node PandaDB

(1) three copies of the PandaDB installation package in three different directories or on three machines.

(2) modify configuration file to enable `cn.pandadb.jraft.enabled` and set `cn.pandadb.jraft.server.peers` on all copies   refer to the following example:

```
cn.pandadb.jraft.enabled=true
cn.pandadb.jraft.server.peers=node1:8081,node2:8081,node3:8081
```

(3) set `cn.pandadb.jraft.server.id`

- modify configuration file on node1

```
cn.pandadb.jraft.server.id=node1:8081
```

- modify configuration file on node2

```
cn.pandadb.jraft.server.id=node2:8081
```

- modify configuration file on node3

```
cn.pandadb.jraft.server.id=node3:8081
```

(4) run below command on three nodes to start cluster

```
bin/neo4j start
```





## 3. Using PandaDB with PandaDB Driver

### 3.1 Download package

visit https://github.com/grapheco/pandadb-v0.2/releases to get pandadb-driver-<version>.jar .

### 3.2 Develop example program

import  dependency first:

```
<dependency>
      <groupId>cn.pandadb</groupId>
      <artifactId>pandadb-driver</artifactId>
      <version>0.2.0-SNAPSHOT</version>
   </dependency>
```

 use `GraphDatabase.driver()` to connect remote PandaDB Cluster, just like using neo4j:

```
// cluster: 127.0.0.1:7610, 127.0.0.1:7620, 127.0.0.1:7630
val driver = GraphDatabase.driver("bolt://127.0.0.1:7610", AuthTokens.basic("neo4j", "neo4j"))

//write or read by session
val session = driver.session()
session.run("create (n:test{name:'test'})")
val res = session.run("match (n) return n")
session.close()

// write or read by transaction
val session2 = driver.session()
val tx = session2.beginTransaction()
tx.run("create (n:test2{name:'test2'})")
val res = tx.run("match (n) return n")
tx.success()
tx.close()
session2.close()
driver.close()
```

  more example code, see https://github.com/grapheco/pandadb-v0.2/tree/master/pandadb-itest/src/main/scala/cn/pandadb/drivertest/



## 4. Licensing

PandaDB v0.2 is an open source product licensed under GPLv3.

