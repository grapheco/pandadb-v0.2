import java.net.URL
import java.time.LocalDate

import org.apache.commons.io.IOUtils
import org.junit.{After, Assert, Before, Test}
import org.neo4j.blob.Blob
import org.neo4j.driver.{AuthTokens, Driver, GraphDatabase, Session}
import scala.collection.JavaConverters._

class DriverTest {
  //    val pandaString2 = s"panda2://127.0.0.1:8081"
  //    val driver = GraphDatabase.driver(pandaString2, AuthTokens.basic("neo4j", "neo4j"))
  //    val session = driver.session()
  //    val tx = session.beginTransaction()
  //    val res = tx.run("return Blob.empty() ~:0.5 Blob.empty() as r")
  //        tx.run("create (n:BBBBBBB{name:'qqqqq'})")
  //        tx.run("create (n:lolololo{name:'test2', blob:<https://www.baidu.com/img/flexible/logo/pc/result.png>})")
  //    val res = tx.run("match (n) return n.name")
  //    while (res.hasNext) {
  //      println(res.next())
  //    }
  //    tx.success()
  //    tx.close()

  //    val tx2 = session.beginTransaction()
  ////    tx2.run("create (n:BBBBBBB{name:'qqqqq'})")
  ////    tx2.run("create (n:lolololo{name:'test2', blob:<https://www.baidu.com/img/flexible/logo/pc/result.png>})")
  //    val res2 = tx2.run("match (n) return n.name")
  //    while (res2.hasNext) {
  //      println(res2.next())
  //    }
  //    tx2.success()
  //    tx2.close()

  val pandaString2 = s"panda2://127.0.0.1:8081"
  var driver: Driver = _
  var session: Session = _

  @Before
  def init(): Unit = {
    driver = GraphDatabase.driver(pandaString2, AuthTokens.basic("neo4j", "neo4j"))
    session = driver.session()
    //    val tx = session.beginTransaction()
    //    tx.run("create (n:bbb{name:'u1', age:100}) return n")
    //    tx.success()
    //    tx.close()
    //    val tx = session.beginTransaction()
    //    tx.run("create (n:Instrument{name:'Guitar', age: 100, date:date(\"2020-06-06\"), blob:<https://www.baidu.com/img/flexible/logo/pc/result.png>} )")
    //    tx.run("create (n:Instrument{name:'Bass', age: 200, date:date(\"2020-06-16\"), blob:<https://www.baidu.com/img/flexible/logo/pc/result.png>} )")
    //    tx.run("create (n:Instrument{name:'Drum', age: 300, date:date(\"2020-06-26\"), blob:<https://www.baidu.com/img/flexible/logo/pc/result.png>} )")
    //    tx.success()
    //    tx.close()
  }

  @Test
  def createBlobTest(): Unit = {
    val tx = session.beginTransaction()
    val res = tx.run("create (n:aaa{name:'test2', blob:<https://www.baidu.com/img/flexible/logo/pc/result.png>}) return n")
    val blob = res.next().get(0).asEntity().get("blob").asBlob().streamSource
    Assert.assertArrayEquals(
      IOUtils.toByteArray(new URL("https://www.baidu.com/img/flexible/logo/pc/result.png")),
      blob.offerStream(IOUtils.toByteArray(_))
    )
    tx.success()
    tx.close()
  }

  @Test
  def createCypherTest(): Unit = {
    val tx = session.beginTransaction()
    tx.run("create (n:aaa{name:'test1', blob:<https://www.baidu.com/img/flexible/logo/pc/result.png>}) return n")
    tx.run("create (n:aaa{name:'test2', age:100, money:1.5, date:date('2020-06-06'), isBoy:true, lst:['a', 'b']}) return n")
    tx.run("create (n:aaa{name:'test3', blob:<https://www.baidu.com/img/flexible/logo/pc/result.png>}) return n")
    val rs = tx.run("match (n:aaa) return n")
    val record = tx.run("match (n:aaa) where n.name = 'test2' return n").next().get(0).asEntity()

    Assert.assertEquals(3L, rs.stream().count())
    //
    Assert.assertEquals("test2", record.get("name").asString())
    Assert.assertEquals(100, record.get("age").asInt())
    assert(record.get("money").asFloat() == 1.5)
    Assert.assertEquals(LocalDate.parse("2020-06-06"), record.get("date").asLocalDate())
    Assert.assertEquals(true, record.get("isBoy").asBoolean())
    Assert.assertEquals(List("a", "b").asJava, record.get("lst").asList())

    tx.success()
    tx.close()
  }

  @Test
  def updateCypherTest(): Unit = {
    val tx = session.beginTransaction()
    val res = tx.run("match (n:bbb) where n.name='u1' set n.age=200  return n").next().get(0)
    println(res)
    //    Assert.assertEquals(200, res.get("age").asInt())
    //    Assert.assertEquals(true, res.get("isBoy").asBoolean())

    tx.success()
    tx.close()
  }


  @Test
  def queryTest(): Unit = {
    val tx = session.beginTransaction()
    val res = tx.run("match (n) return n")
    while (res.hasNext) {
      val record = res.next()
      println(record, record.get(0).asEntity().get("name"))
    }
  }

  @After
  def close(): Unit = {
    val tx = session.beginTransaction()
    tx.run("match (n) detach delete n")
    tx.success()
    tx.close()
    session.close()
    driver.close()
  }
}
