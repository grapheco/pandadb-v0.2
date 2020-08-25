package concurrent

import org.junit.{After, Assert, Before, Test}
import org.neo4j.driver.{AuthTokens, Driver, GraphDatabase, StatementResult}

import scala.collection.mutable.ArrayBuffer
import scala.util.Random

class ConcurrentTest {
  val pandaString2 = s"panda2://127.0.0.1:8081"
  val drivers = ArrayBuffer[Driver]()
  val driverNumber = 50
  val testWriteTimes = 1
  val driver4Test = GraphDatabase.driver(pandaString2, AuthTokens.basic("neo4j", "neo4j"))

  for (i <- 1 to driverNumber) {
    drivers += GraphDatabase.driver(pandaString2, AuthTokens.basic("neo4j", "neo4j"))
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

  //  @Before
  //  def close(): Unit = {
  //    val tx = driver4Test.session().beginTransaction()
  //    tx.run("match (n) detach delete n")
  //    tx.success()
  //    tx.close()
  //    driver4Test.close()
  //  }
}
