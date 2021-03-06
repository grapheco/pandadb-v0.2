package cn.pandadb.drivertest

import java.io.File
import cn.pandadb.itest.dis.ServerBootStrap
import org.apache.commons.io.FileUtils
import org.junit.{After, Assert, Before, Test}
import org.neo4j.driver.{AuthTokens, Driver, GraphDatabase, Session, Transaction}

// before test, run at least 3 servers in itest.PandaJraftServerTest
class driverTest {
  val clusterUri = "bolt://127.0.0.1:7610"
  var driver: Driver = _
  var session: Session = _
  var tx: Transaction = _

//  @Before
//  def init(): Unit = {
//    if (new File("./testoutput").exists()) {
//      FileUtils.deleteDirectory(new File("./testoutput"))
//    }
//  }

  @Test
  def testSession(): Unit = {
      driver = GraphDatabase.driver(clusterUri, AuthTokens.basic("neo4j", "neo4j"))
      session = driver.session()
      session.run("create (n:a{num:1})")
      val res1 = session.run("match (n) return n").stream().count()
      session.run("create (n:a{num:2})")
      val res2 = session.run("match (n) return n").stream().count()
      Assert.assertEquals(1, res1.toInt)
      Assert.assertEquals(2, res2.toInt)
  }

  @Test
  def testTransaction(): Unit = {
      driver = GraphDatabase.driver(clusterUri, AuthTokens.basic("neo4j", "neo4j"))
      session = driver.session()
      val tempTx = session.beginTransaction()
      tempTx.run("create (n:a{num:0})")
      tempTx.success()
      tempTx.close()
      tx = session.beginTransaction()
      tx.run("create (n:b{num:2})")
      tx.run("create (n:c{num:3})")
      val res = tx.run("match (n) return n").stream().count()
      Assert.assertEquals(3, res.toInt)
      tx.success()
      tx.close()
  }

  @Test
  def testSessionWithTransactionSituation(): Unit = {
      driver = GraphDatabase.driver(clusterUri, AuthTokens.basic("neo4j", "neo4j"))
      session = driver.session()
      session.run("create (n:a{num:1})")
      tx = session.beginTransaction()
      tx.run("create (n:b{num:2})")
      tx.run("create (n:c{num:3})")
      val res = tx.run("match (n) return n").stream().count()
      Assert.assertEquals(3, res.toInt)
      tx.success()
      tx.close()
  }

  @After
  def close: Unit = {
    session.close()
    driver.close()
  }
}

