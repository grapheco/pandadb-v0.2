package cn.pandadb.itest

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
import org.neo4j.driver.{AuthTokens, Driver, GraphDatabase, Session}
import org.neo4j.server.CommunityBootstrapper

import scala.collection.JavaConverters._

// make sure jraft.enabled = false
class CypherVerification {
  //val pandaString2 = s"bolt://10.0.82.217:8076"
  val pandaString2 = s"bolt://127.0.0.1:7610"
  var driver: PandaDriver = _
  var neo4jServer1: CommunityBootstrapper = _

  def startServer1(): Unit = {
    neo4jServer1 = new CommunityBootstrapper
    val confFile: File = new File("./testinput/single.conf")

    val dbFile = Paths.get("./testoutput", "data1").toFile()

    neo4jServer1.start(dbFile, Optional.of(confFile), new util.HashMap[String, String])

    println("confile================" + confFile.getAbsolutePath)

    val config = PandaRuntimeContext.contextGet[PandaConfig]()
    if (config.useJraft) {
      while (PandaRuntimeContext.contextGet[PandaJraftService]().jraftServer.getNode.getLeaderId == null) {
        println("no leader")
        Thread.sleep(500)
      }
      println(PandaRuntimeContext.contextGet[PandaJraftService]().jraftServer.getNode.getLeaderId)
    }
  }

  @Before
  def init(): Unit = {
    if (new File("./testoutput").exists()) {
      FileUtils.deleteDirectory(new File("./testoutput"))
    }

    startServer1()
    driver = PandaDriver.create(pandaString2, "neo4j", "neo4j")
  }

  @Test
  def creation(): Unit = {
    val session = driver.writeSession()
    val tx = session.beginTransaction()
    tx.run("CREATE (n:Person {age: 10, name: 'bob', address: 'CNIC, CAS, Beijing, China'})")
    tx.run("CREATE (n:Person {age: 10, name: 'bob2', address: 'CNIC, CAS, Beijing, China'})")
    tx.run("CREATE (n:Person {age: 40, name: 'alex', address: 'CNIC, CAS, Beijing, China'})")
    tx.run("CREATE (n:Person {age: 40, name: 'alex2', address: 'CNIC, CAS, Beijing, China'})")
    tx.run("CREATE INDEX ON :Person(address)")
    tx.run("CREATE INDEX ON :Person(name)")
    tx.run("CREATE INDEX ON :Person(age)")
    tx.run("CREATE INDEX ON :Person(name, age)")
    tx.run("match (f:Person), (s:Person) where f.age=40 AND s.age=10 CREATE (f)-[hood:Father]->(s)")
    tx.success()
    tx.close()

    val tx2 = session.beginTransaction()
    val res2 = tx2.run("match (n:Person) return count(n)").next().get(0).asEntity()
    Assert.assertEquals(4, res2.containsKey("count(n)"))
    tx2.success()
    tx2.close()
  }

  @Test
  def merge(): Unit = {
    val session = driver.writeSession()
    val tx = session.beginTransaction()
    tx.run("MERGE (user:User { Id: 456 })")
    tx.success()
    tx.close()
    val tx2 = session.beginTransaction()
    val res2 = tx2.run("match (user:User {Id: 456}) return user").next().get(0).asEntity()
    Assert.assertEquals(456, res2.get("Id").asInt())
    tx2.success()
    tx2.close()
    session.close()
  }

  @Test
  def setProperty(): Unit = {
    val session = driver.writeSession()
    val tx = session.beginTransaction()
    tx.run("MERGE (user:User { Id: 456 }) ON CREATE SET user.Name = 'Jim'")
    tx.success()
    tx.close()
    val tx2 = session.beginTransaction()
    val res2 = tx2.run("match (user:User {Id: 456, Name:'Jim'}) return user").next().get(0).asEntity()
    Assert.assertEquals("Jim", res2.get("Name").asString())
    tx2.success()
    tx2.close()
    session.close()
  }

  @Test
  def removeProperty(): Unit = {
    val session = driver.writeSession()
    val tx = session.beginTransaction()
    tx.run("MERGE (user:User { Id: 456 }) ON CREATE SET user.Name = 'Jim'")
    tx.success()
    tx.close()
    val tx2 = session.beginTransaction()
    val res2 = tx2.run("match (user:User { Id: 456 }) remove user.Name return user").next().get(0).asEntity()
    Assert.assertEquals(true, res2.containsKey("Id"))
    Assert.assertEquals(false, res2.containsKey("Name"))
    tx2.success()
    tx2.close()
    session.close()
  }

  @Test
  def delete(): Unit = {
    val session = driver.writeSession()
    val tx = session.beginTransaction()
    tx.run("MERGE (user:User { Id: 456 }) ON CREATE SET user.Name = 'Jim'")
    tx.run("MATCH (user:User) WHERE user.Id = 456 DELETE user")
    tx.success()
    tx.close()
    val tx2 = session.beginTransaction()
    Assert.assertEquals(false, tx2.run("match (user:User {Id: 456, Name:'Jim'}) return user").hasNext())
    tx2.success()
    tx2.close()
    session.close()
  }

  //ask Hipro if need pointType
  @Test
  def pointType(): Unit = {
    val session = driver.writeSession()
    val tx = session.beginTransaction()
    tx.run("CREATE (n:Person {loc: point({ x:3, y:4 }), salary: 40.5, name: 'blue', isLeader: true, address: 'CNIC, CAS, Beijing, China'})")
    tx.success()
    tx.close()
    val tx2 = session.beginTransaction()
    val res2 = tx2.run("match (n:Person) WHERE n.loc.x = 3 AND n.salary = 40.5 return n").next().get(0).asEntity()
    Assert.assertEquals(40.5, res2.get("salary").asFloat())
    tx2.success()
    tx2.close()
    session.close()
    session.close()
  }

  @Test
  def listType(): Unit = {
    val session = driver.writeSession()
    val tx = session.beginTransaction()
    tx.run("CREATE (n:Test {check: [1997,2003,2003,2000,1999,2003,1995], name: 'blue'})")
    tx.run("match (n:Test) WHERE n.check = [1997,2003,2003,2000,1999,2003,1995] AND n.name = 'blue' return count(n)")
    tx.success()
    tx.close()
    session.close()
  }

  @Test
  def dateType(): Unit = {
    val session = driver.writeSession()
    val tx = session.beginTransaction()
    tx.run("CREATE (n:Person {birthday: datetime('1975-06-24T12:50:35.556+0100'), salary: 40.5, name: 'blue', isLeader: true, address: 'CNIC, CAS, Beijing, China'})")
    tx.run("match (n:Person) WHERE n.birthday = datetime('1975-06-24T12:50:35.556+0100') AND n.salary = 40.5 return count(n)")
    tx.run("CREATE (n:Test {timeTest: localtime('12:50:35.556'), name: 'blue'})")
    tx.run("match (n:Test) WHERE n.timeTest = localtime('12:50:35.556') AND n.name = 'blue' return count(n)")
    tx.run("CREATE (n:Test {timeTest: time('125035.556+0100'), name: 'blue'})")
    tx.run("match (n:Test) WHERE n.timeTest = time('125035.556+0100') AND n.name = 'blue' return count(n)")
    tx.success()
    tx.close()
    session.close()
  }


  //need support schemaless. Ask HiPro to check if schemaless must support
  @Test
  def costore(): Unit = {
    val session = driver.writeSession()
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
      """CREATE (ToyStory4:Movie {title:'Toy Story 4', released:2019})
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
  }

  @Test
  def createBlob(): Unit = {
    val session = driver.writeSession()
    val tx = session.beginTransaction()
    val res = tx.run("create (n:aaa{name:'test_blob', blob:<https://www.baidu.com/img/flexible/logo/pc/result.png>}) return n")

    val blob = res.next().get(0).asEntity().get("blob").asBlob().streamSource
    Assert.assertArrayEquals(
      IOUtils.toByteArray(new URL("https://www.baidu.com/img/flexible/logo/pc/result.png")),
      blob.offerStream(IOUtils.toByteArray(_))
    )
    tx.success()
    tx.close()
    session.close()
  }

  @Test
  def deleteBlob(): Unit = {
    val session = driver.writeSession()
    val tx = session.beginTransaction()
    tx.run("create (n:delete_blob{name:'test_blob', blob:<https://www.baidu.com/img/flexible/logo/pc/result.png>}) return n")
    tx.success()
    tx.close()

    val tx2 = session.beginTransaction()
    val res2 = tx2.run("match (n:delete_blob) where n.name='test_blob' remove n.blob return n").next().get(0).asEntity()
    Assert.assertEquals(false, res2.containsKey("blob"))
    tx2.success()
    tx2.close()
    session.close()

  }

  @Test
  def cypherPlusTest(): Unit = {
    val session = driver.writeSession()
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
    session.close()

  }

  @Test
  def esError(): Unit = {
    val session = driver.writeSession()
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

  @Test
  def cypherPlusError(): Unit = {
    val session = driver.readSession()
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
  }

  @Test
  def blobTxTest(): Unit = {
    val session = driver.writeSession()
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

  @After
  def close: Unit = {
    driver.close()
    neo4jServer1.stop()
  }
}