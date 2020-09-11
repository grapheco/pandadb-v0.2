package cn.pandadb.drivertest

import java.io.File
import java.nio.file.Paths
import java.util
import java.util.Optional

import cn.pandadb.config.PandaConfig
import cn.pandadb.itest.dis.ServerBootStrap
import cn.pandadb.jraft.PandaJraftService
import cn.pandadb.server.PandaRuntimeContext
import org.apache.commons.io.FileUtils
import org.junit.{After, Assert, Before, Test}
import org.neo4j.driver.{AuthTokens, Driver, GraphDatabase, StatementResult}
import org.neo4j.server.CommunityBootstrapper

import scala.collection.mutable.ArrayBuffer
import scala.util.Random

// make sure all the jraft.enabled = true
class DriverTest4Concurrent {
  val pandaString = s"bolt://127.0.0.1:7610"
  val drivers = ArrayBuffer[Driver]()
  val driverNumber = 10
  val testWriteTimes = 1
  var driver4Test: Driver = null
  val sp = new ServerBootStrap

  @Before
  def init(): Unit = {
    if (new File("./testoutput").exists()) {
      FileUtils.deleteDirectory(new File("./testoutput"))
    }
    sp.startThreeNodes()
    Thread.sleep(10000)

    driver4Test = GraphDatabase.driver(pandaString, AuthTokens.basic("neo4j", "neo4j"))
    for (i <- 1 to driverNumber) {
      drivers += GraphDatabase.driver(pandaString, AuthTokens.basic("neo4j", "neo4j"))
    }
  }

  @Test
  def testWrite(): Unit = {
    try {
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
    } catch {
      case exception: Exception => sp.stopAllnodes()
    }
  }

  def createNode(driver: Driver, times: Int): Unit = {
    val tx = driver.session().beginTransaction()
    for (i <- 1 to times) {
      val r = Random.nextInt(1000000).toString
      tx.run(s"create (n:aaa{name:'${r}', age:100, money:1.5, date:date('2020-06-06'), isBoy:true, lst:['a', 'b']})")
    }
    tx.success()
    tx.close()
    driver.close()
  }
}

