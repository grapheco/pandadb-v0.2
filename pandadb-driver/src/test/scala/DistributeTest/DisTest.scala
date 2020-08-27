//package DistributeTest
//
//import java.io.{File, FileInputStream}
//import java.net.URL
//import java.time.LocalDate
//
//import com.alipay.sofa.jraft.RouteTable
//import org.apache.commons.io.IOUtils
//import org.junit.{After, Assert, Before, Test}
//import org.neo4j.driver.{AuthTokens, Driver, GraphDatabase, Session}
//import scala.collection.JavaConverters._
//
//class DisTest {
//
//  val pandaString2 = s"panda2://127.0.0.1:8081"
//  val uui = s"127.0.0.1:8081,127.0.0.1:8082,127.0.0.1:8083"
//  var rt = RouteTable.getInstance()
//  var driver: Driver = _
//  var session: Session = _
//  var driver2: Driver = _
//  var sessionR: Session = _
//
//  @Before
//  def init(): Unit = {
//    driver = GraphDatabase.driver(pandaString2, AuthTokens.basic("neo4j", "neo4j"))
//    session = driver.session()
//    //sessionW = SelectNode.getDriver( AuthTokens.basic("neo4j", "neo4j"), true, rt, uui).session()
//    sessionR = SelectNode.getDriver( AuthTokens.basic("neo4j", "neo4j"), false, rt, uui).session()
//  }
//
//  @Test
//  def createBlobTest(): Unit = {
//    val tx = session.beginTransaction()
//    val res = tx.run("create (n:aaa{name:'test_blob', blob:<https://www.baidu.com/img/flexible/logo/pc/result.png>}) return n")
//    val blob = res.next().get(0).asEntity().get("blob").asBlob().streamSource
//    Assert.assertArrayEquals(
//      IOUtils.toByteArray(new URL("https://www.baidu.com/img/flexible/logo/pc/result.png")),
//      blob.offerStream(IOUtils.toByteArray(_))
//    )
//
//    tx.success()
//    tx.close()
//    Thread.sleep(10000)
//    //read test
//    val txr = sessionR.beginTransaction()
//    val resr = txr.run("match(n) where n.name = 'test_blob' return n")
//    val blob2 = resr.next().get(0).asEntity().get("blob").asBlob().streamSource
//    Assert.assertArrayEquals(
//      IOUtils.toByteArray(new URL("https://www.baidu.com/img/flexible/logo/pc/result.png")),
//      blob2.offerStream(IOUtils.toByteArray(_))
//    )
//
//    txr.success()
//    txr.close()
//    //read test
//  }
//
//  @Test
//  def deleteBlobTest(): Unit = {
//    val tx = session.beginTransaction()
//    tx.run("create (n:delete_blob{name:'test_blob', blob:<https://www.baidu.com/img/flexible/logo/pc/result.png>}) return n")
//    tx.success()
//    tx.close()
//
//    val tx2 = sessionR.beginTransaction()
//    val res2 = tx2.run("match (n:delete_blob) where n.name='test_blob' remove n.blob return n").next().get(0).asEntity()
//    Assert.assertEquals(false, res2.containsKey("blob"))
//    tx2.success()
//    tx2.close()
//  }
//
//  @Test
//  def createCypherTest(): Unit = {
//    val tx = session.beginTransaction()
//    val record1 = tx.run("create (n:aaa{name:'test2', age:100, money:1.5, date:date('2020-06-06'), isBoy:true, lst:['a', 'b']}) return n").next().get(0).asEntity()
//    tx.success()
//    tx.close()
//    //read test
//    val tx2 = sessionR.beginTransaction()
//    val record = tx2.run("match(n) where n.name = 'test2' return n").next().get(0).asEntity()
//    //read test
//    Assert.assertEquals("test2", record.get("name").asString())
//    Assert.assertEquals(100, record.get("age").asInt())
//    Assert.assertTrue(record.get("money").asFloat() == 1.5)
//    Assert.assertEquals(LocalDate.parse("2020-06-06"), record.get("date").asLocalDate())
//    Assert.assertEquals(true, record.get("isBoy").asBoolean())
//    Assert.assertEquals(List("a", "b").asJava, record.get("lst").asList())
//
//    tx2.success()
//    tx2.close()
//
//  }
//
//  @Test
//  def updateCypherTest(): Unit = {
//    var tx = session.beginTransaction()
//    tx.run("create (n:bbb{name:'u1', age:100})")
//    val res1 = tx.run("match (n:bbb) where n.name='u1' set n.age=200 set n.isBoy=true return n").next().get(0).asEntity()
//    tx.success()
//    tx.close()
//
//    val tx2 = sessionR.beginTransaction()
//    val res = tx2.run("match (n:bbb) where n.name='u1' return n").next().get(0).asEntity()
//    //update node properties
//    Assert.assertEquals(200, res.get("age").asInt())
//    Assert.assertEquals(true, res.get("isBoy").asBoolean())
//
//    tx = session.beginTransaction()
//    //update node labels
//    tx.run("match (n:bbb) where n.name='u1' set n:Person")
//    val res3 = tx.run("match (n:bbb) where n.name='u1' set n:People:Star return labels(n)").next().get(0).asList()
//    tx.success()
//    tx.close()
//
//    val res2 = tx2.run("match (n:bbb) where n.name='u1' return labels(n)").next().get(0).asList()
//    Assert.assertEquals(List("bbb", "Person", "People", "Star").asJava, res2)
//  }
//
//  @Test
//  def deletePropertyTest(): Unit = {
//    val tx = session.beginTransaction()
//    val res1 = tx.run("create (n:label1:label2:label3{name:'u1', age1:1, age2:2, age3:3}) return n").next().get(0).asEntity()
//    Assert.assertEquals(true, res1.containsKey("age2"))
//    Assert.assertEquals(true, res1.containsKey("age3"))
//
//    val res3 = tx.run("match (n:label1) where n.name='u1' remove n.age3, n.age2 return n").next().get(0).asEntity()
//    tx.success()
//    tx.close()
//    val tx2 = sessionR.beginTransaction()
//    val res2 = tx2.run("match(n:label1) where n.name='u1' return n").next().get(0).asEntity()
//
//    Assert.assertEquals(true, res2.containsKey("age1"))
//    Assert.assertEquals(false, res2.containsKey("age2"))
//    Assert.assertEquals(false, res2.containsKey("age3"))
//  }
//
//  @Test
//  def relationshipTest(): Unit = {
//    val tx = session.beginTransaction()
//
//    //way 1
//    tx.run("create (n:band{name:'Wu Tiao Ren', company:'Modern Sky'}) return n")
//    tx.run("create (n:person{name:'仁科', skill:'Accordion, Guitar', blob:<https://pic1.zhimg.com/v2-b3a20c939f1a8d9e0b01a8f0af0192f5_1440w.jpg>})")
//    tx.run("create (n:person{name:'阿茂', skill:'Folk Guitar'})")
//    tx.run(
//      """match (a:person{name:'仁科'}), (b:person{name:'阿茂'}), (c:band{name:'Wu Tiao Ren'})
//        |merge (a)-[r1:friend{name:'test'}]->(b)
//        |merge (b)-[r2:friend{name:'test'}]->(a)
//        |merge (a)-[r3:belong{name:'test'}]->(c)
//        |merge (b)-[r4:belong{name:'test'}]->(c)""".stripMargin)
//    //way 2
//    tx.run(
//      """create (n:person{name:'杜凯'})-[:partner{name:'test'}]->(nn:person{name:'刘恋'})
//        |create (nn)-[:partner{name:'test'}]->(n)
//        |create (n)-[:belong{name:'test'}]->(b:band{name:'MrMiss'})
//        |create (nn)-[:belong{name:'test'}]->(b)""".stripMargin)
//    tx.success()
//    tx.close()
//
//    val tx2 = sessionR.beginTransaction()
//    val re = tx2.run("match (n:person{name:'仁科'})-[r]->(nn:person{name:'阿茂'}) return type(r)").next()
//    Assert.assertEquals("friend", re.get(0).asString())
//
//    val re2 = tx2.run("match (n:person{name:'刘恋'})-[r]->(nn:band{name:'MrMiss'}) return type(r)").next()
//    Assert.assertEquals("belong", re2.get(0).asString())
//
//    tx2.success()
//    tx2.close()
//
//  }
//
//  @After
//  def close(): Unit = {
//    val tx = session.beginTransaction()
//    tx.run("match (n) detach delete n")
//    tx.success()
//    tx.close()
//    session.close()
//    driver.close()
//  }
//
//}
