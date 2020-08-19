import java.io.{File, FileInputStream}
import java.net.URL
import java.time.LocalDate

import org.apache.commons.io.IOUtils
import org.junit.{After, Assert, Before, Test}
import org.neo4j.blob.Blob
import org.neo4j.driver.{AuthTokens, Driver, GraphDatabase, Session}

import scala.collection.JavaConverters._

class DriverTest {
  val pandaString2 = s"panda2://127.0.0.1:8081"
  var driver: Driver = _
  var session: Session = _

  @Before
  def init(): Unit = {
    driver = GraphDatabase.driver(pandaString2, AuthTokens.basic("neo4j", "neo4j"))
    session = driver.session()
  }

  @Test
  def createAndDeleteBlobTest(): Unit = {
    val tx = session.beginTransaction()
    val res = tx.run("create (n:aaa{name:'test_blob', blob:<https://www.baidu.com/img/flexible/logo/pc/result.png>}) return n")

    val blob = res.next().get(0).asEntity().get("blob").asBlob().streamSource
    Assert.assertArrayEquals(
      IOUtils.toByteArray(new URL("https://www.baidu.com/img/flexible/logo/pc/result.png")),
      blob.offerStream(IOUtils.toByteArray(_))
    )

    val res2 = tx.run("match (n:aaa) where n.name='test_blob' remove n.blob return n").next().get(0).asEntity()
    Assert.assertEquals(false, res2.containsKey("blob"))

    tx.success()
    tx.close()
  }

  @Test
  def createCypherTest(): Unit = {
    val tx = session.beginTransaction()
    val record = tx.run("create (n:aaa{name:'test2', age:100, money:1.5, date:date('2020-06-06'), isBoy:true, lst:['a', 'b']}) return n").next().get(0).asEntity()

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
    tx.run("create (n:bbb{name:'u1', age:100})")
    val res = tx.run("match (n:bbb) where n.name='u1' set n.age=200 set n.isBoy=true return n").next().get(0).asEntity()

    //update node properties
    Assert.assertEquals(200, res.get("age").asInt())
    Assert.assertEquals(true, res.get("isBoy").asBoolean())

    //update node labels
    tx.run("match (n:bbb) where n.name='u1' set n:Person")
    val res2 = tx.run("match (n:bbb) where n.name='u1' set n:People:Star return labels(n)").next().get(0).asList()

    Assert.assertEquals(List("bbb", "Person", "People", "Star").asJava, res2)

    tx.success()
    tx.close()
  }

  @Test
  def relationshipTest(): Unit = {
    val tx = session.beginTransaction()

    //way 1
    tx.run("create (n:band{name:'Wu Tiao Ren', company:'Modern Sky'}) return n")
    tx.run("create (n:person{name:'仁科', skill:'Accordion, Guitar', blob:<https://pic1.zhimg.com/v2-b3a20c939f1a8d9e0b01a8f0af0192f5_1440w.jpg>})")
    tx.run("create (n:person{name:'阿茂', skill:'Folk Guitar'})")
    tx.run(
      """match (a:person{name:'仁科'}), (b:person{name:'阿茂'}), (c:band{name:'Wu Tiao Ren'})
        |merge (a)-[r1:friend{name:'test'}]->(b)
        |merge (b)-[r2:friend{name:'test'}]->(a)
        |merge (a)-[r3:belong{name:'test'}]->(c)
        |merge (b)-[r4:belong{name:'test'}]->(c)""".stripMargin)
    //way 2
    tx.run(
      """create (n:person{name:'杜凯'})-[:partner{name:'test'}]->(nn:person{name:'刘恋'})
        |create (nn)-[:partner{name:'test'}]->(n)
        |create (n)-[:belong{name:'test'}]->(b:band{name:'MrMiss'})
        |create (nn)-[:belong{name:'test'}]->(b)""".stripMargin)

    val re = tx.run("match (n:person{name:'仁科'})-[r]->(nn:person{name:'阿茂'}) return type(r)").next()
    Assert.assertEquals("friend", re.get(0).asString())

    val re2 = tx.run("match (n:person{name:'刘恋'})-[r]->(nn:band{name:'MrMiss'}) return type(r)").next()
    Assert.assertEquals("belong", re2.get(0).asString())

    tx.success()
    tx.close()

  }

  @Test
  def cypherPlusTest(): Unit = {
    val tx = session.beginTransaction()
    val blob1 = tx.run("return <https://www.baidu.com/img/flexible/logo/pc/result.png> as r").next().get("r").asBlob()

    Assert.assertArrayEquals(IOUtils.toByteArray(new URL("https://www.baidu.com/img/flexible/logo/pc/result.png")),
      blob1.offerStream {
        IOUtils.toByteArray(_)
      })

    Assert.assertTrue(blob1.length > 0)

    val basedir = new File("../hbase-blob-storage/testinput/ai").getCanonicalFile.getAbsolutePath
    val blob2 = tx.run(s"return <file://${basedir}/bluejoe1.jpg> as r").next().get("r").asBlob()
    Assert.assertArrayEquals(IOUtils.toByteArray(new FileInputStream(new File(basedir, "bluejoe1.jpg"))),
      blob2.offerStream {
        IOUtils.toByteArray(_)
      })

    Assert.assertTrue(blob2.length > 0)

    Assert.assertEquals(true, tx.run("return Blob.empty() ~:0.5 Blob.empty() as r").next().get("r").asBoolean());
    Assert.assertEquals(true, tx.run("return Blob.empty() ~:0.5 Blob.empty() as r").next().get("r").asBoolean());
    Assert.assertEquals(true, tx.run("return Blob.empty() ~:1.0 Blob.empty() as r").next().get("r").asBoolean());
    Assert.assertEquals(true, tx.run("return Blob.empty() ~: Blob.empty() as r").next().get("r").asBoolean());

    Assert.assertEquals(true, tx.run(
      s"return <file://${basedir}/bluejoe1.jpg> ~: <file://${basedir}/bluejoe2.jpg> as r")
      .next().get("r").asBoolean());

    Assert.assertTrue(tx.run("return '翟天临' :: '天临 翟' as r").next().get("r").asDouble() > 0.7);
    Assert.assertTrue(tx.run("return '翟天临' :: '天临 翟' as r").next().get("r").asDouble() < 0.8);
    Assert.assertTrue(tx.run("return '翟天临' ::jaro '天临 翟' as r").next().get("r").asDouble() > 0.7);
    Assert.assertEquals(true, tx.run("return '翟天临' ~: '天临 翟' as r").next().get("r").asBoolean());
    Assert.assertEquals(true, tx.run("return '翟天临' ~:jaro/0.7 '天临 翟' as r").next().get("r").asBoolean());
    Assert.assertEquals(false, tx.run("return '翟天临' ~:jaro/0.8 '天临 翟' as r").next().get("r").asBoolean());

    Assert.assertEquals(new File(basedir, "bluejoe1.jpg").length(),
      tx.run(s"return <file://${basedir}/bluejoe1.jpg> ->length as x")
        .next().get("x").asLong());

    Assert.assertEquals("image/jpeg", tx.run(s"return <file://${basedir}/bluejoe1.jpg>->mime as x")
      .next().get("x").asString());

    Assert.assertEquals(3968, tx.run(s"return <file://${basedir}/bluejoe1.jpg>->width as x")
      .next().get("x").asInt());

    Assert.assertEquals(2976, tx.run(s"return <file://${basedir}/bluejoe1.jpg>->height as x")
      .next().get("x").asInt());

    tx.success()
    tx.close()
  }

  //  @Test
  //  def bugReplay(): Unit ={
  //    val tx = session.beginTransaction()
  //    Assert.assertEquals(1.toLong, tx.run("return 1 :: 2 as r").next().get("r"))
  //    tx.success()
  //    tx.close()
  //  }

  //  @Test
  //  def query(): Unit = {
  //    val tx = session.beginTransaction()
  //    val res = tx.run("match (n) return n")
  //    while (res.hasNext) {
  //      val record = res.next().get(0).asEntity()
  //      println(record.get("name").asString())
  //    }
  //    tx.success()
  //    tx.close()
  //  }

  @Test
  def delete(): Unit = {
    val tx = session.beginTransaction()
    tx.run("match (n) detach delete n")
    tx.success()
    tx.close()
    session.close()
    driver.close()
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
