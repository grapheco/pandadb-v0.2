package cn.pandadb.itest

import java.io.File
import java.nio.file.Paths
import java.util
import java.util.{HashMap, Optional}

import cn.pandadb.config.PandaConfig
import cn.pandadb.jraft.PandaJraftServer
import cn.pandadb.server.PandaRuntimeContext
import org.neo4j.graphdb.factory.GraphDatabaseFactory
import org.neo4j.server.CommunityBootstrapper
import cn.pandadb.jraft.PandaJraftService
import org.neo4j.graphdb.GraphDatabaseService

object PandaJraftTest1 {

  def main(args: Array[String]): Unit = {
    val neo4jServer: CommunityBootstrapper = new CommunityBootstrapper
    val confFile: File = new File("testinput/test1.conf")

    val dbFile = Paths.get("/testoutput", "data1").toFile()
    neo4jServer.start(dbFile, Optional.of(confFile), new util.HashMap[String, String])
    val config = PandaRuntimeContext.contextGet[PandaConfig]()
    if (config.useJraft) {
      while (PandaRuntimeContext.contextGet[PandaJraftService]().jraftServer.getNode.getLeaderId==null){
        println("no leader")
        Thread.sleep(500)
      }
      println(PandaRuntimeContext.contextGet[PandaJraftService]().jraftServer.getNode.getLeaderId)
    }

  }

}

object PandaJraftTest2 {

  def main(args: Array[String]): Unit = {
    val neo4jServer: CommunityBootstrapper = new CommunityBootstrapper
    val confFile: File = new File("testinput/test2.conf")

    val dbFile = Paths.get("/testoutput", "data2").toFile()
    neo4jServer.start(dbFile, Optional.of(confFile), new util.HashMap[String, String])
  }

}

object PandaJraftTest3 {

  def main(args: Array[String]): Unit = {
    val neo4jServer: CommunityBootstrapper = new CommunityBootstrapper
    val confFile: File = new File("testinput/test3.conf")

    val dbFile = Paths.get("/testoutput", "data3").toFile()
    neo4jServer.start(dbFile, Optional.of(confFile), new util.HashMap[String, String])

  }

}

object Test4 {
  def main (args: Array[String] ): Unit = {
    val dbFile = new File("./output/testdb")
    val confFile = new File("testinput/test1.conf")
    //        neo4jServer.start(dbFile, Optional.of(confFile), new HashMap<String,String>());
    val db = new GraphDatabaseFactory().newEmbeddedDatabaseBuilder(dbFile).loadPropertiesFromFile(confFile.getPath).newGraphDatabase
    db.execute("create (n:person{name:'test2', blob:<https://www.baidu.com/img/flexible/logo/pc/result.png>})")
}
}
