package cn.pandadb.itest.dis

import java.io.{BufferedReader, File, InputStreamReader}
import java.nio.file.Paths
import java.util
import java.util.Optional

import cn.pandadb.config.PandaConfig
import cn.pandadb.itest.{PandaJraftTest1, PandaJraftTest2, PandaJraftTest3}
import cn.pandadb.jraft.PandaJraftService
import cn.pandadb.server.PandaRuntimeContext
import org.apache.commons.io.FileUtils
import org.apache.commons.lang.StringUtils

import sys.process._
import java.lang.Process

import org.junit.Test
import org.neo4j.server.CommunityBootstrapper

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer
import scala.util.Random

object GetCmdLine {
  //final val  path = "F:\\IdCode\\pandadb-v0.2\\pandadb-raft-itest\\runServer.txt"
  final val  path = "./runServer.txt"
  def main(args: Array[String]): Unit = {
    saveResult
    println("prepare envirement!!!!!!")
    println(loadResult().split(" ").length)
  }
  def saveResult(): Unit = {
    val res = getResult().toString
    val resList = res.split("CommandLine=")
    resList.foreach(u => {
      if (u.contains("cn.pandadb.itest.dis.RunServer")) save(u)
    })
  }
  def save(s: String): Unit = {
    val file = new File(path)
    val res = s.split(" ").toBuffer
    res -= res.last
    //println(res.mkString(" "))
    FileUtils.writeStringToFile(file, res.mkString(" "))
  }
  def loadResult(): String = {
    var file = new File(path)
    if (!file.exists()) {
      saveResult()
      file = new File(path)
    }
    val str = FileUtils.readFileToString(file)
    str
  }
  def getResult(): String = {
    "wmic process where caption=\"java.exe\" get commandline /value"!!
  }

}
class NodeInfo(conFile: String, dbPath: String, dbFileName: String) {
  var id: Int = _
  var process: Process = null
  var isStart: Boolean = false
  def getConFile(): String = {
    this.conFile
  }
  def getdbPath(): String = {
    this.dbPath
  }
  def getdbFileName(): String = {
    this.dbFileName
  }
  def setId(ids: Int): Unit = {
    this.id = ids
  }
  def setProcess(p: Process): Unit = {
    this.process = p
  }
  def setStart(): Unit = {
    this.isStart = true
  }
  def setStop(): Unit = {
    this.isStart = false
    this.process = null
  }
}

class ServerBootStrap() {
  val cmdStr: String = GetCmdLine.loadResult()
  var nodeId: Int = 0
  var nodeProcessMap = mutable.Map[Int, Process]()

  def startNode(nodeInfo: NodeInfo): NodeInfo = {
    val id: Int = getId()
    var tempP: Process = null
    val thread = new Thread(new Runnable {
      override def run(): Unit = {
        val p = runServer(nodeInfo.getConFile(), nodeInfo.getdbPath(), nodeInfo.getdbFileName())
        tempP = p

        //nodeProcessMap  += id -> p
        val stdout = new BufferedReader(new InputStreamReader(p.getInputStream()))

        var line: String = null
        while ((line = stdout.readLine()) != null && p.isAlive) {
          //Thread.sleep(1000)
          println(line)
        }
        println(s"server ${id} has stoped!!!!")
      }
    })
    thread.start()
    while (tempP == null) {
      Thread.sleep(500)

    }
    nodeProcessMap  += id -> tempP
    nodeInfo.setId(id)
    nodeInfo.setProcess(tempP)
    nodeInfo.setStart()
    //while(thread.getState ==Thread.State.RUNNABLE)
    //println(nodeProcessMap.size)
    nodeInfo
  }

  def startNode(conFile: String, dbPath: String, dbFileName: String): Int = {
    val id: Int = getId()
    var tempP: Process = null
    val thread = new Thread(new Runnable {
      override def run(): Unit = {
        val p = runServer(conFile, dbPath, dbFileName)
        tempP = p

        //nodeProcessMap  += id -> p
        val stdout = new BufferedReader(new InputStreamReader(p.getInputStream()))

        var line: String = null
        while ((line = stdout.readLine()) != null && p.isAlive) {
          //Thread.sleep(1000)
          println(line)
        }
        println(s"server ${id} has stoped!!!!")
      }
    })
    thread.start()
    while (tempP == null) {
      Thread.sleep(500)

    }
    nodeProcessMap  += id -> tempP
    //while(thread.getState ==Thread.State.RUNNABLE)
    //println(nodeProcessMap.size)
    id
  }
  def getId(): Int = {
    this.nodeId = this.nodeId + 1
    this.nodeId
  }
  def stopNode(id: Int): Boolean = {
    val p = nodeProcessMap.get(id).get
    if (p != None) {
      p.destroy()
      nodeProcessMap.remove(id)
      println("==*****stop Node*****==")
      println(s"==*****node id is ${id}*****==")
      println("==*****stop Node*****==")
      p.isAlive
    }
    else true
  }
  def stopNode(nodeInfo: NodeInfo): Boolean = {
    val p = nodeProcessMap.get(nodeInfo.id).get
    if (p != None) {
      p.destroy()
      nodeInfo.setStop()
      nodeProcessMap.remove(nodeInfo.id)
      println("==*****stop Node*****==")
      println(s"==*****node id is ${nodeInfo.id}*****==")
      println("==*****stop Node*****==")
      p.isAlive
    }
    else true
  }
  def randomStopNode(): Int = {
    var retId: Int = 0
    if (nodeProcessMap.size <= 0) {
      println("there is no node runing")
      retId = -1
    }
    else {
      val ids = nodeProcessMap.keys.toArray
      val idtsp = ids(Random.nextInt(ids.size))
      stopNode(idtsp)
      retId = idtsp
    }
    retId
  }

  def runServer(conFile: String, dbPath: String, dbFileName: String): Process = {
    val cmdLine = s"${cmdStr} ${conFile},${dbPath},${dbFileName}"
    val p = Runtime.getRuntime.exec(cmdLine)
    //this.tempP = p
    p
  }

  def startThreeNodes(): Unit = {
    //val sp = new ServerBootStrap
    val confile = "./testinput/test1.conf"
    val confile2 = "./testinput/test2.conf"
    val confile3 = "./testinput/test3.conf"
    val dbPath = "./testoutput"
    val dbFileName = "data1"
    val dbFileName2 = "data2"
    val dbFileName3 = "data3"
    val id1 = startNode(confile, dbPath, dbFileName)

    val id2 = startNode(confile2, dbPath, dbFileName2)
    val id3 = startNode(confile3, dbPath, dbFileName3)
    Thread.sleep(10000)
  }

  def stopAllnodes(): Unit = {
    nodeProcessMap.keys.foreach(u => stopNode(u))
  }
  def getAllNodesId(): List[Int] = {
    nodeProcessMap.keys.toList
  }

}

