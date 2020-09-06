package cn.pandadb.itest.dis

import org.junit.Test

class MultiNodeTest {
  @Test
  def tes(): Unit = {
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

  }
}