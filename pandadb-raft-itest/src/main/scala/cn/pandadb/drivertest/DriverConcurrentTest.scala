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
import org.neo4j.driver.{AuthTokens, Driver, GraphDatabase, StatementResult}
import org.neo4j.server.CommunityBootstrapper

import scala.collection.mutable.ArrayBuffer
import scala.util.Random

class DriverConcurrentTest {
  val pandaString2 = s"panda2://127.0.0.1:8081"
  val drivers = ArrayBuffer[Driver]()
  val driverNumber = 50
  val testWriteTimes = 1
  val driver4Test = GraphDatabase.driver(pandaString2, AuthTokens.basic("neo4j", "neo4j"))

  val neo4jServer1: CommunityBootstrapper = new CommunityBootstrapper
  val neo4jServer2: CommunityBootstrapper = new CommunityBootstrapper
  val neo4jServer3: CommunityBootstrapper = new CommunityBootstrapper


  for (i <- 1 to driverNumber) {
    drivers += GraphDatabase.driver(pandaString2, AuthTokens.basic("neo4j", "neo4j"))
  }

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
  }

  @Test
  def testWrite(): Unit = {
    drivers.par.foreach(
      driver => {
        createNode(driver, testWriteTimes)
      }
    )

    val tx = driver4Test.session().beginTransaction()
    val res = tx.run("match (n) return n")
    Assert.assertEquals(testWriteTimes * drivers.size, res.stream().count())
    tx.success()
    tx.close()
  }

  @Test
  def testRead(): Unit = {
    drivers.par.foreach(
      driver => {
        queryNode(driver)
      }
    )
  }

  def createNode(driver: Driver, times: Int): Unit = {
    val tx = driver.session().beginTransaction()
    for (i <- 1 to times) {
      val r = Random.nextInt(1000000).toString
      tx.run(s"create (n:aaa{name:'${r}', age:100, money:1.5, date:date('2020-06-06'), isBoy:true, lst:['a', 'b']})")
      println("ok", i)
    }
    tx.success()
    tx.close()
    driver.close()
  }

  def queryNode(driver: Driver): Unit = {
    val tx = driver.session().beginTransaction()
    val res = tx.run("match (n) return n")
    println(res.stream().count())
    tx.close()
  }
}

