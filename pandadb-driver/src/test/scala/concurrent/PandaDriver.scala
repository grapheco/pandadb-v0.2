package concurrent

import org.neo4j.driver.{AuthTokens, Driver, GraphDatabase, Session}

import scala.util.Random

class PandaDriver {
  val pandaString2 = s"panda2://127.0.0.1:8081"
  val driver = GraphDatabase.driver(pandaString2, AuthTokens.basic("neo4j", "neo4j"))

  def createNode(times: Int): Unit = {
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

}
