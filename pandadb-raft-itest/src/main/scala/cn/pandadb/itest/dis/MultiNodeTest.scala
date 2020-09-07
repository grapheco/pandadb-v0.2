package cn.pandadb.itest.dis

import java.io.File
import java.nio.file.Paths

import org.junit.{After, Assert, Before, Test}
import org.neo4j.driver.{AuthTokens, GraphDatabase}

class MultiNodeTest {
  final val pathsr = "F:\\IdCode\\pandadb-v0.2\\pandadb-raft-itest\\testoutput"
  @Before
  def delDirectory(): Unit = {
    val path = Paths.get(pathsr)
    delDir(path.toFile)
  }
  @After
  def removeDir(): Unit = {
    val path = Paths.get(pathsr)
    delDir(path.toFile)
  }

  def delDir(dir: File): Unit = {
    dir.listFiles().foreach(file => {
      if (file.isDirectory) delDir(file)
      else file.delete()
    })
  }
  @Test
  def tesSingleNode(): Unit = {
    val sp = new ServerBootStrap
    val confile = "./testinput/test1.conf"
    val confile2 = "./testinput/test2.conf"
    val confile3 = "./testinput/test3.conf"
    val dbPath = "./testoutput"
    val dbFileName = "data1"
    val dbFileName2 = "data2"
    val dbFileName3 = "data3"
    val id1 = sp.startNode(confile, dbPath, dbFileName)

    val id2 = sp.startNode(confile2, dbPath, dbFileName2)
    val id3 = sp.startNode(confile3, dbPath, dbFileName3)

    Thread.sleep(20000)
    while (sp.randomStopNode() >=0) {
      Thread.sleep(20000)
    }

    val id4 = sp.startNode(confile, dbPath, dbFileName)
    Thread.sleep(20000)
    println(id4)
    sp.stopNode(id4)
  }

  @Test
  def testStartAllnodes(): Unit = {
    val sp = new ServerBootStrap
    sp.startThreeNodes()
    Thread.sleep(10000)
    sp.getAllNodesId().foreach(u => println(u))
    sp.stopAllnodes()
  }

  @Test
  def testWR(): Unit = {
    val sp = new ServerBootStrap
    sp.startThreeNodes()
    Thread.sleep(10000)

    val bolt1 = s"bolt://127.0.0.1:7610"
    val bolt2 = s"bolt://127.0.0.1:7620"
    val driver = GraphDatabase.driver(bolt1, AuthTokens.basic("neo4j", "neo4j"))
    val session = driver.session()
    val driver2 = GraphDatabase.driver(bolt2, AuthTokens.basic("neo4j", "neo4j"))
    val session2 = driver2.session()
    val tx = session.beginTransaction()
    tx.run("create(n:node{name:'haha'}) return n")
    tx.success()
    tx.close()
    session.close()

    val res = session2.run("match(n:node) return n").next().get(0).asEntity().get("name")
    Assert.assertEquals("haha", res.asString())
    println(res.asString())
    session2.close()
    sp.stopAllnodes()

  }

  @Test
  def testOnBrowser(): Unit = {
    val sp = new ServerBootStrap
    sp.startThreeNodes()
    var blf = true
    Thread.sleep(10000)
    sp.stopAllnodes()
  }
}