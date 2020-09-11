package cn.pandadb.drivertest

import java.io.File
import java.nio.file.Paths
import java.util
import java.util.Optional

import cn.pandadb.config.PandaConfig
import cn.pandadb.jraft.PandaJraftService
import cn.pandadb.server.PandaRuntimeContext
import org.apache.commons.io.FileUtils
import org.junit.{After, Assert, Before, Test}
import org.neo4j.driver.{AuthToken, AuthTokens, Driver, GraphDatabase}
import org.neo4j.server.CommunityBootstrapper

class DriverBlobTest {
  val pandaString = s"bolt://127.0.0.1:7610"
  var driver: Driver = _

  val neo4jServer1: CommunityBootstrapper = new CommunityBootstrapper
  val neo4jServer2: CommunityBootstrapper = new CommunityBootstrapper
  val neo4jServer3: CommunityBootstrapper = new CommunityBootstrapper

  def startServer(server: CommunityBootstrapper, confName: String, dbName: String, flag: Int = 0): Unit = {
    val confFile: File = new File(s"./testinput/$confName.conf")

    val dbFile = Paths.get("./testoutput", s"$dbName").toFile()

    server.start(dbFile, Optional.of(confFile), new util.HashMap[String, String])

    println("confile================" + confFile.getPath)

    if (flag == 1) {
      val config = PandaRuntimeContext.contextGet[PandaConfig]()
      if (config.useJraft) {
        while (PandaRuntimeContext.contextGet[PandaJraftService]().jraftServer.getNode.getLeaderId == null) {
          println("no leader")
          Thread.sleep(500)
        }
        println(PandaRuntimeContext.contextGet[PandaJraftService]().jraftServer.getNode.getLeaderId)
      }
    }
  }

  @Before
  def init(): Unit = {

    if (new File("./testoutput").exists()) {
      FileUtils.deleteDirectory(new File("./testoutput"))
    }
    startServer(neo4jServer1, "test1", "data1")
    startServer(neo4jServer2, "test2", "data2")
    startServer(neo4jServer3, "test3", "data3", 1)

    driver = GraphDatabase.driver(pandaString, AuthTokens.basic("neo4j", "neo4j"))
  }

  @Test
  def testCreateMutiBlobsInNode(): Unit = {
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
  }

  @Test
  def testRemoveMutiBlobsInNode(): Unit = {
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
  }

  @Test
  def testUpdateMutiBlobsInNode(): Unit = {
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
  }

  @Test
  def testOverrideBlobInNode(): Unit = {
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
  }

  @After
  def close(): Unit = {
    //    val session = driver.writeSession()
    //    val tx = session.beginTransaction()
    //    tx.run("match (n) detach delete n") // for delete blobs
    //    tx.success()
    //    tx.close()
    //    session.close()
    driver.close()
    neo4jServer1.stop()
    neo4jServer2.stop()
    neo4jServer3.stop()
  }
}
