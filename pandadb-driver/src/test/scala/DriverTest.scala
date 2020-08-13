import org.neo4j.driver.{AuthTokens, GraphDatabase}

object DriverTest {
  def main(args: Array[String]): Unit = {
    val pandaString2 = s"panda2://127.0.0.1:8082"
    val driver = GraphDatabase.driver(pandaString2, AuthTokens.basic("", ""))
    val session = driver.session()
    val tx = session.beginTransaction()
    //    tx.run("create (n:BBBBBBB{name:'qqqqq'})")
    //    tx.run("create (n:bbbbb{name:'test2', blob:<https://www.baidu.com/img/flexible/logo/pc/result.png>})")
    val res = tx.run("match (n) return n.name")
    while (res.hasNext) {
      println(res.next())
    }
    tx.success()
    tx.close()
    session.close()
    driver.close()
  }
}
