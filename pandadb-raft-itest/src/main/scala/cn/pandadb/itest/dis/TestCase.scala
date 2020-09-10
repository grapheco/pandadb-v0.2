package cn.pandadb.itest.dis

import java.io.{File, FileInputStream}
import java.net.URL
import java.nio.file.Paths
import java.time.LocalDate
import java.util
import java.util.Optional

import cn.pandadb.config.PandaConfig
import cn.pandadb.driver.v2.PandaDriver
import cn.pandadb.jraft.PandaJraftService
import cn.pandadb.server.PandaRuntimeContext
import org.apache.commons.io.{FileUtils, IOUtils}
import org.junit.{After, Assert, Before, Test}
import org.neo4j.driver
import org.neo4j.driver.{AuthTokens, Driver, GraphDatabase, Session}
import org.neo4j.server.CommunityBootstrapper

import scala.collection.JavaConverters._

// make sure jraft.enabled = true
class TCase(pandaString2: String) {
  //val pandaString2 = s"panda://127.0.0.1:8081"
  var driver: Driver = _
  var session: Session = _

  def startTest(): Unit = {
    driver = GraphDatabase.driver(pandaString2, AuthTokens.basic("neo4j", "neo4j"))
  }

  def createBlobTest(): Unit = {
    val session = driver.session()
    val tx = session.beginTransaction()
    val res = tx.run("create (n:aaa{name:'test_blob', blob:<https://www.baidu.com/img/flexible/logo/pc/result.png>}) return n")

    val blob = res.next().get(0).asEntity().get("blob").asBlob().streamSource
    Assert.assertArrayEquals(
      IOUtils.toByteArray(new URL("https://www.baidu.com/img/flexible/logo/pc/result.png")),
      blob.offerStream(IOUtils.toByteArray(_))
    )

    tx.success()
    tx.close()
  }

  def deleteBlobTest(): Unit = {
    val session = driver.session()
    val tx = session.beginTransaction()
    tx.run("create (n:delete_blob{name:'test_blob', blob:<https://www.baidu.com/img/flexible/logo/pc/result.png>}) return n")
    tx.success()
    tx.close()

    val tx2 = session.beginTransaction()
    val res2 = tx2.run("match (n:delete_blob) where n.name='test_blob' remove n.blob return n").next().get(0).asEntity()
    Assert.assertEquals(false, res2.containsKey("blob"))
    tx2.success()
    tx2.close()
  }

  def createCypherTest(): Unit = {
    val session = driver.session()
    val tx = session.beginTransaction()
    val record = tx.run("create (n:aaa{name:'test2', age:100, money:1.5, date:date('2020-06-06'), isBoy:true, lst:['a', 'b']}) return n").next().get(0).asEntity()

    Assert.assertEquals("test2", record.get("name").asString())
    Assert.assertEquals(100, record.get("age").asInt())
    Assert.assertTrue(record.get("money").asFloat() == 1.5)
    Assert.assertEquals(LocalDate.parse("2020-06-06"), record.get("date").asLocalDate())
    Assert.assertEquals(true, record.get("isBoy").asBoolean())
    Assert.assertEquals(List("a", "b").asJava, record.get("lst").asList())

    tx.success()
    tx.close()
  }

  def updateCypherTest(): Unit = {
    val session = driver.session()
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

    //delete node label
    val res3 = tx.run("match (n:bbb) where n.name='u1' remove n:People:Star return labels(n)").next().get(0).asList()
    Assert.assertEquals(List("bbb", "Person").asJava, res3)
    tx.success()
    tx.close()
  }

  def deletePropertyTest(): Unit = {
    val session = driver.session()
    val tx = session.beginTransaction()
    val res1 = tx.run("create (n:label1:label2:label3{name:'u1', age1:1, age2:2, age3:3}) return n").next().get(0).asEntity()
    Assert.assertEquals(true, res1.containsKey("age2"))
    Assert.assertEquals(true, res1.containsKey("age3"))

    val res2 = tx.run("match (n:label1) where n.name='u1' remove n.age3, n.age2 return n").next().get(0).asEntity()
    Assert.assertEquals(true, res2.containsKey("age1"))
    Assert.assertEquals(false, res2.containsKey("age2"))
    Assert.assertEquals(false, res2.containsKey("age3"))
  }

  def relationshipTest(): Unit = {
    val session = driver.session()
    val tx = session.beginTransaction()

    //way 1
    tx.run("create (n:band{name:'Wu Tiao Ren', company:'Modern Sky'}) return n")
    tx.run("create (n:person{name:'仁科', skill:'Accordion, Guitar'})")
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

  def cypherPlusTest(): Unit = {
    val session = driver.session()
    val tx = session.beginTransaction()
    val blob1 = tx.run("return <https://www.baidu.com/img/flexible/logo/pc/result.png> as r").next().get("r").asBlob()

    Assert.assertTrue(blob1.length > 0)

    Assert.assertArrayEquals(IOUtils.toByteArray(new URL("https://www.baidu.com/img/flexible/logo/pc/result.png")),
      blob1.offerStream {
        IOUtils.toByteArray(_)
      })

    val basedir = new File("../hbase-blob-storage/testinput/ai").getCanonicalFile.getAbsolutePath
    val blob2 = tx.run(s"return <file://${basedir}/test1.png> as r").next().get("r").asBlob()

    Assert.assertTrue(blob2.length > 0)

    Assert.assertArrayEquals(IOUtils.toByteArray(new FileInputStream(new File(basedir, "test1.png"))),
      blob2.offerStream {
        IOUtils.toByteArray(_)
      })

    Assert.assertEquals(true, tx.run("return Blob.empty() ~:0.5 Blob.empty() as r").next().get("r").asBoolean());
    Assert.assertEquals(true, tx.run("return Blob.empty() ~:0.5 Blob.empty() as r").next().get("r").asBoolean());
    Assert.assertEquals(true, tx.run("return Blob.empty() ~:1.0 Blob.empty() as r").next().get("r").asBoolean());
    Assert.assertEquals(true, tx.run("return Blob.empty() ~: Blob.empty() as r").next().get("r").asBoolean());

    Assert.assertEquals(true, tx.run(
      s"return <file://${basedir}/bluejoe2.jpg> ~: <file://${basedir}/bluejoe2.jpg> as r")
      .next().get("r").asBoolean());

    //    Assert.assertTrue(tx.run("return '孙悟空' :: '悟空 孙' as r").next().get("r").asDouble() > 0.7);
    //    Assert.assertTrue(tx.run("return '孙悟空' :: '悟空 孙' as r").next().get("r").asDouble() < 0.8);
    //    Assert.assertTrue(tx.run("return '孙悟空' ::jaro '悟空 孙' as r").next().get("r").asDouble() > 0.7);
    //    Assert.assertEquals(true, tx.run("return '孙悟空' ~: '悟空 孙' as r").next().get("r").asBoolean());
    //    Assert.assertEquals(true, tx.run("return '孙悟空' ~:jaro/0.7 '悟空 孙' as r").next().get("r").asBoolean());
    //    Assert.assertEquals(false, tx.run("return '孙悟空' ~:jaro/0.8 '悟空 孙' as r").next().get("r").asBoolean());

    Assert.assertEquals(new File(basedir, "bluejoe2.jpg").length(),
      tx.run(s"return <file://${basedir}/bluejoe2.jpg> ->length as x")
        .next().get("x").asLong());

    Assert.assertEquals("image/jpeg", tx.run(s"return <file://${basedir}/bluejoe2.jpg>->mime as x")
      .next().get("x").asString());

    Assert.assertEquals(4032, tx.run(s"return <file://${basedir}/bluejoe2.jpg>->width as x")
      .next().get("x").asInt());

    Assert.assertEquals(3024, tx.run(s"return <file://${basedir}/bluejoe2.jpg>->height as x")
      .next().get("x").asInt());

    tx.success()
    tx.close()
  }

  def esError(): Unit = {
    val session = driver.session()
    val tx = session.beginTransaction()
    tx.run(
      """CREATE (TheMatrix:Movie {title:'The Matrix', released:1999, tagline:'Welcome to the Real World'})
        |CREATE (Keanu:Person {name:'Keanu Reeves', born:1964})
        |CREATE (Carrie:Person {name:'Carrie-Anne Moss', born:1967})
        |CREATE (Laurence:Person {name:'Laurence Fishburne', born:1961})
        |CREATE (Hugo:Person {name:'Hugo Weaving', born:1960})
        |CREATE (LillyW:Person {name:'Lilly Wachowski', born:1967})
        |CREATE (LanaW:Person {name:'Lana Wachowski', born:1965})
        |CREATE (JoelS:Person {name:'Joel Silver', born:1952})
        |CREATE (Keanu)-[:ACTED_IN {roles:['Neo']}]-> (TheMatrix),
        |(Carrie)-[:ACTED_IN {roles:['Trinity']}]-> (TheMatrix),
        |(Laurence)-[:ACTED_IN {roles:['Morpheus']}]-> (TheMatrix),
        |(Hugo)-[:ACTED_IN {roles:['Agent Smith']}]-> (TheMatrix),
        |(LillyW)-[:DIRECTED]-> (TheMatrix),
        |(LanaW)-[:DIRECTED]-> (TheMatrix),
        |(JoelS)-[:PRODUCED]-> (TheMatrix)""".stripMargin
    )
    tx.success()
    tx.close()
    val tx2 = session.beginTransaction()
    tx2.run(
      """match (n:Movie) where n.title='The Matrix' remove n.tagline
        |CREATE (ToyStory4:Movie {title:'Toy Story 4', released:2019})
        |MERGE (Keanu:Person {name:'Keanu Reeves', born:1964})
        |SET Keanu.wonOscar = false, Keanu.filmDebut = 1985
        |MERGE (TomH:Person {name:'Tom Hanks', born:1956})
        |SET TomH.wonOscar = true, TomH.filmDebut = 1980
        |MERGE (TimA:Person {name:'Tim Allen', born:1953})
        |SET TimA.wonOscar = false, TimA.filmDebut = '1988 maybe?'
        |MERGE (AnnieP:Person {name:'Annie Potts', born:1952})
        |SET AnnieP.wonOscar = false, AnnieP.filmDebut = 1978
        |CREATE (Keanu)-[:ACTED_IN {roles:['Duke Caboom (voice)']}]-> (ToyStory4),
        |(TomH)-[:ACTED_IN {roles:['Woody (voice)']}]-> (ToyStory4),
        |(TimA)-[:ACTED_IN {roles:['Buzz Lightyear (voice)']}]-> (ToyStory4),
        |(AnnieP)-[:ACTED_IN {roles:['Bo Peep (voice)']}]-> (ToyStory4)""".stripMargin
    )
    tx2.success()
    tx2.close()
    session.close()

  }

  def cypherPlusError(): Unit = {
    val session = driver.session()
    val basedir = new File("../hbase-blob-storage/testinput/ai").getCanonicalFile.getAbsolutePath

    val tx = session.beginTransaction()

    Assert.assertEquals(false, tx.run(
      s"return <file://${basedir}/cat1.jpg> ~: <file://${basedir}/dog1.jpg> as r")
      .next().get("r").asBoolean());

    Assert.assertEquals(true, tx.run(
      s"return <file://${basedir}/bluejoe2.jpg> ~: <file://${basedir}/bluejoe2.jpg> as r")
      .next().get("r").asBoolean());

    tx.success()
    tx.close()
    session.close()

  }

  def blobTxTest(): Unit = {
    val session = driver.session()
    val tx = session.beginTransaction()
    tx.run("create (n:bbb{name:'test_blob', age:10, blob:<https://www.baidu.com/img/flexible/logo/pc/result.png>}) return n").next().get(0).asEntity()
    val res2 = tx.run("match (n:bbb) where n.name='test_blob' remove n.blob return n ").next().get(0).asEntity()
    Assert.assertEquals("NULL", res2.get("blob").toString)
    Assert.assertEquals("NULL", res2.get("whatever").toString)
    Assert.assertEquals("test_blob", res2.get("name").asString())
    tx.success()
    tx.close()
    session.close()
  }

  def stopTest(): Unit = {
    driver.close()
  }

}


