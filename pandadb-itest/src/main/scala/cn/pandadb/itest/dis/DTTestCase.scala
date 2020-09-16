package cn.pandadb.itest.dis

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
import org.neo4j.driver.{AuthTokens, Driver, GraphDatabase, Session, Transaction}
import org.neo4j.server.CommunityBootstrapper

class DTTestCase(clusterUri: String) {
  //val clusterUri = "bolt://127.0.0.1:7610"
 // val sp = new ServerBootStrap
  var driver: Driver = _
  var session: Session = _
  var tx: Transaction = _

  def init(): Unit = {
  }

  def testSession(): Unit = {

      driver = GraphDatabase.driver(clusterUri, AuthTokens.basic("neo4j", "neo4j"))
      session = driver.session()
      session.run("match(n) delete n")
      session.run("create (n:a{num:1})")
      val res1 = session.run("match (n) return n").stream().count()
      session.run("create (n:a{num:2})")
      val res2 = session.run("match (n) return n").stream().count()
      Assert.assertEquals(1, res1.toInt)
      Assert.assertEquals(2, res2.toInt)
     session.run("match(n) delete n")
    session.close()
    driver.close()
  }

  def testTransaction(): Unit = {
      driver = GraphDatabase.driver(clusterUri, AuthTokens.basic("neo4j", "neo4j"))
      session = driver.session()
      val tempTx = session.beginTransaction()
      tempTx.run("match(n) delete n")
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
    session.close()
    driver.close()
  }

  def testSessionWithTransactionSituation(): Unit = {
      driver = GraphDatabase.driver(clusterUri, AuthTokens.basic("neo4j", "neo4j"))
      session = driver.session()
      session.run("match(n) delete n")
      session.run("create (n:a{num:1})")
      tx = session.beginTransaction()
      tx.run("create (n:b{num:2})")
      tx.run("create (n:c{num:3})")
      val res = tx.run("match (n) return n").stream().count()
      Assert.assertEquals(3, res.toInt)
      tx.success()
      tx.close()
    session.close()
    driver.close()
  }

  def stopTest: Unit = {
    //session.close()
    //driver.close()
    //sp.stopAllnodes()
  }
}

