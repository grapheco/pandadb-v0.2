package cn.pandadb.itest

import java.io.File
import java.nio.file.Paths

import cn.pandadb.jraft.PandaJraftServer
import org.neo4j.graphdb.factory.GraphDatabaseFactory

object PandaJraftTest1 {

  def main(args: Array[String]): Unit = {
    val dbFile = Paths.get("tmp", "testdata1", "db", "test.db").toFile()
    val jraftDataPath = Paths.get("tmp", "testdata1", "jraft").toString

    val localNeo4jDB = new GraphDatabaseFactory().newEmbeddedDatabaseBuilder(dbFile).newGraphDatabase
    val jraftServer = PandaJraftServer.apply(localNeo4jDB, jraftDataPath, "pandadb",
      "127.0.0.1:8081", "127.0.0.1:8081,127.0.0.1:8082,127.0.0.1:8083")
    jraftServer.start()

    while (jraftServer.getNode.getLeaderId == null) {
      println("==== no leader ====")
      Thread.sleep(500)
    }

    println(jraftServer.isLeader)

    if (jraftServer.isLeader) {
      println("fffffffffffffff")
      val tx = localNeo4jDB.beginTx()
      localNeo4jDB.execute("CREATE(n:TEST {name:'TEST-NAME', age:1})")
      localNeo4jDB.execute("CREATE(n:TEST2 {name:'TEST-NAME2', age:2})")
      tx.success()
      tx.close()
    }
    println(jraftServer.getNode.getLeaderId)

  }

}

object PandaJraftTest2 {

  def main(args: Array[String]): Unit = {
    val dbFile = Paths.get("tmp", "testdata2", "db", "test.db").toFile()
    val jraftDataPath = Paths.get("tmp", "testdata2", "jraft").toString

    val localNeo4jDB = new GraphDatabaseFactory().newEmbeddedDatabaseBuilder(dbFile).newGraphDatabase
    val jraftServer = PandaJraftServer.apply(localNeo4jDB, jraftDataPath, "pandadb",
      "127.0.0.1:8082", "127.0.0.1:8081,127.0.0.1:8082,127.0.0.1:8083")
    jraftServer.start()

    while (jraftServer.getNode.getLeaderId == null){
      println("==== no leader ====")
      Thread.sleep(500)
    }

    if (jraftServer.getNode.isLeader) {
      val tx = localNeo4jDB.beginTx()
      localNeo4jDB.execute("CREATE(n:TEST {name:'TEST-NAME', age:1})")
      localNeo4jDB.execute("CREATE(n:TEST2 {name:'TEST-NAME2', age:2})")
      tx.success()
      tx.close()
    }
    println(jraftServer.getNode.getLeaderId)
  }

}

object PandaJraftTest3 {

  def main(args: Array[String]): Unit = {
    val dbFile = Paths.get("tmp", "testdata3", "db", "test.db").toFile()
    val jraftDataPath = Paths.get("tmp", "testdata3", "jraft").toString

    val localNeo4jDB = new GraphDatabaseFactory().newEmbeddedDatabaseBuilder(dbFile).newGraphDatabase
    val jraftServer = PandaJraftServer.apply(localNeo4jDB, jraftDataPath, "pandadb",
      "127.0.0.1:8083", "127.0.0.1:8081,127.0.0.1:8082,127.0.0.1:8083")
    jraftServer.start()

    while (jraftServer.getNode.getLeaderId == null){
      println("==== no leader ====")
      Thread.sleep(500)
    }

    if (jraftServer.getNode.isLeader) {
      val tx = localNeo4jDB.beginTx()
      localNeo4jDB.execute("CREATE(n:TEST {name:'TEST-NAME', age:1})")
      localNeo4jDB.execute("CREATE(n:TEST2 {name:'TEST-NAME2', age:2})")
      tx.success()
      tx.close()
    }

    println(jraftServer.getNode.getLeaderId)

  }

}
