### PandaDB Manual
#### 1. How to package
```
1. cd pandadb-driver
2. mvn clean package
```
this command will create `panda-driver-<version>.jar` in `target` directory.
#### 2. Usage
 Note: pandadb-driver compatible with neo4j-driver's standalone mode.
 - - -  
 **2.1  pandadb-cluster mode:**  
 If you have a pandadb-cluster, you can create driver by using any cluster's IP and bolt port.  
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
 **2.2  standalone mode:**  
Note: just use as neo4j-driver, you can only create driver by using the server's IP and bolt port.  
 
