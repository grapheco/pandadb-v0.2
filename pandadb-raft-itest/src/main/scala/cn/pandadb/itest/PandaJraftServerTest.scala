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

object PandaJraftTest1 {

  def main(args: Array[String]): Unit = {
    val neo4jServer: CommunityBootstrapper = new CommunityBootstrapper
    val confFile: File = new File("testinput/test1.conf")

    val dbFile = Paths.get("/testoutput", "data1").toFile()
    neo4jServer.start(dbFile, Optional.of(confFile), new util.HashMap[String, String])
    println(PandaRuntimeContext.contextGet[PandaConfig]())
    while (PandaRuntimeContext.contextGet[PandaJraftService]().jraftServer.getNode.getLeaderId==null){
      println("no leader")
      Thread.sleep(500)
    }
    println(PandaRuntimeContext.contextGet[PandaJraftService]().jraftServer.getNode.getLeaderId)
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
