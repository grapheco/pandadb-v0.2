package cn.pandadb.itest.dis

import java.io.File
import java.nio.file.Paths
import java.util
import java.util.Optional

import cn.pandadb.config.PandaConfig
import cn.pandadb.jraft.PandaJraftService
import cn.pandadb.server.PandaRuntimeContext
import org.neo4j.server.CommunityBootstrapper

object RunServer {

  def main(args: Array[String]): Unit = {
    //./testinput/test1.conf,./testoutput,data1
    val argList = args(0).split(",")
    if (argList.length !=3) {
      println("argument is eroor, please set right argument!!!")
      return
    }
    val confile = argList(0)
    val outPath = argList(1)
    val dataPath = argList(2)
   // println("confile,outpath,datapath====" + confile)
   // println("confile,outpath,datapath=====" + outPath)
   // println("confile,outpath,datapath======" + dataPath)
    val neo4jServer: CommunityBootstrapper = new CommunityBootstrapper
    val confFile: File = new File(confile)

    val dbFile = Paths.get(outPath, dataPath).toFile()

    neo4jServer.start(dbFile, Optional.of(confFile), new util.HashMap[String, String])


    println("confile================" + confFile.getPath)
    println("=======*******%%%%%%%%%%%%%***********=========" )
    println("=======*******%%%%%%%%%%%%%***********=========" )
    println("=======*******%%%%%%%%%%%%%***********=========" )

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