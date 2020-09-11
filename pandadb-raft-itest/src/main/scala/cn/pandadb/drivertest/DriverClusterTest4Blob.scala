package cn.pandadb.drivertest

import java.io.File

import cn.pandadb.itest.dis.ServerBootStrap
import org.apache.commons.io.FileUtils
import org.junit.{After, Assert, Before, Test}
import org.neo4j.driver.{AuthTokens, Driver, GraphDatabase}

class DriverClusterTest4Blob {
  val pandaString = s"bolt://127.0.0.1:7610"
  var driver: Driver = _
  val sp = new ServerBootStrap

  @Before
  def init(): Unit = {
    if (new File("./testoutput").exists()) {
      FileUtils.deleteDirectory(new File("./testoutput"))
    }
    sp.startThreeNodes()
    Thread.sleep(5000)
    driver = GraphDatabase.driver(pandaString, AuthTokens.basic("neo4j", "neo4j"))
  }

  @Test
  def testCreateMutiBlobsInNode(): Unit = {
    try {
      val session = driver.session()
      val tx = session.beginTransaction()
      val res = tx.run(
        """create (n:person{name:'mblob',
          |blob1:<https://www.baidu.com/img/flexible/logo/pc/result.png>,
          |blob2:<https://raw.githubusercontent.com/LianxinGao/Keep_On_Growing/master/images/blob2.jpg>,
          |blob3:<https://raw.githubusercontent.com/LianxinGao/Keep_On_Growing/master/images/blob3.jpg>})
          |return n""".stripMargin
      ).next().get(0).asEntity()
      Assert.assertEquals(6617, res.get("blob1").asBlob().length)
      Assert.assertEquals(40432, res.get("blob2").asBlob().length)
      Assert.assertEquals(31604, res.get("blob3").asBlob().length)
      tx.success()
      tx.close()
      session.close()
    } catch {
      case exception: Exception => sp.stopAllnodes()
    }
  }

  @Test
  def testRemoveMutiBlobsInNode(): Unit = {
    try {
      val session = driver.session()
      val tx = session.beginTransaction()
      tx.run(
        """create (n:person{name:'mblob',
          |blob1:<https://www.baidu.com/img/flexible/logo/pc/result.png>,
          |blob2:<https://raw.githubusercontent.com/LianxinGao/Keep_On_Growing/master/images/blob3.jpg>,
          |blob3:<https://raw.githubusercontent.com/LianxinGao/Keep_On_Growing/master/images/blob2.jpg>})
      """.stripMargin
      )
      tx.success()
      tx.close()

      val tx2 = session.beginTransaction()
      val res = tx2.run("match (n:person) where n.name='mblob' remove n.blob1, n.blob2 return n").next().get(0).asEntity()
      Assert.assertEquals(false, res.containsKey("blob1"))
      Assert.assertEquals(false, res.containsKey("blob2"))
      Assert.assertEquals(true, res.containsKey("blob3"))
      tx2.success()
      tx2.close()
      session.close()
    } catch {
      case exception: Exception => sp.stopAllnodes()
    }
  }

  @Test
  def testUpdateMutiBlobsInNode(): Unit = {
    try {
      val session = driver.session()
      val tx = session.beginTransaction()
      tx.run("create (n:person{name:'up_blob'})")
      val res = tx.run(
        """match (n:person) where n.name='up_blob'
          |set n.blob1 = <https://www.baidu.com/img/flexible/logo/pc/result.png>
          |set n.blob2 = <https://raw.githubusercontent.com/LianxinGao/Keep_On_Growing/master/images/blob3.jpg>
          |return n""".stripMargin
      ).next().get(0).asEntity()
      Assert.assertEquals(true, res.containsKey("blob1"))
      Assert.assertEquals(true, res.containsKey("blob2"))
      Assert.assertEquals(6617, res.get("blob1").asBlob().length)
      Assert.assertEquals(31604, res.get("blob2").asBlob().length)
      tx.success()
      tx.close()
      session.close()
    } catch {
      case exception: Exception => sp.stopAllnodes()
    }
  }

  @Test
  def testOverrideBlobInNode(): Unit = {
    try {
      // should delete the origin blob.
      val session = driver.session()
      val tx = session.beginTransaction()
      tx.run("create (n:person{name:'up_blob',blob1:<https://www.baidu.com/img/flexible/logo/pc/result.png>})")
      tx.success()
      tx.close()

      val tx2 = session.beginTransaction()
      val res = tx2.run(
        """match (n:person) where n.name='up_blob'
          |set n.blob1 = <https://raw.githubusercontent.com/LianxinGao/Keep_On_Growing/master/images/blob3.jpg>
          |return n""".stripMargin
      ).next().get(0).asEntity()
      Assert.assertEquals(true, res.containsKey("blob1"))
      Assert.assertEquals(31604, res.get("blob1").asBlob().length)
      tx2.success()
      tx2.close()
      session.close()
    } catch {
      case exception: Exception => sp.stopAllnodes()
    }
  }
  @After
  def close(): Unit = {
    driver.close()
    sp.stopAllnodes()
  }
}
