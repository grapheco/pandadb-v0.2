package cn.pandadb.itest

import java.io.File
import java.nio.file.Paths
import java.util
import java.util.{HashMap, Optional}

import cn.pandadb.config.PandaConfig
import cn.pandadb.costore.{ExternalPropertiesContext, InElasticSearchPropertyNodeStore}
import cn.pandadb.jraft.PandaJraftServer
import cn.pandadb.server.PandaRuntimeContext
import org.neo4j.graphdb.factory.GraphDatabaseFactory
import org.neo4j.server.CommunityBootstrapper
import cn.pandadb.jraft.PandaJraftService
import java.io.{File, FileInputStream}
import java.util.Properties

import cn.pandadb.costore.{CustomPropertyNodeStore, ExternalPropertiesContext}
import org.junit.{After, Before, Test}
import org.neo4j.graphdb.GraphDatabaseService
import org.neo4j.io.fs.FileUtils

class PandaCostoreTest {

  val neo4jServer: CommunityBootstrapper = new CommunityBootstrapper
  val confFile: File = new File("testinput/test-costore.conf")
  neo4jServer.start(Paths.get("testoutput", "testdb").toFile(), Optional.of(confFile), new util.HashMap[String, String])
  println(PandaRuntimeContext.contextGet[PandaConfig]())
  val esNodeStore: InElasticSearchPropertyNodeStore = PandaRuntimeContext.contextGet[CustomPropertyNodeStore]().asInstanceOf[InElasticSearchPropertyNodeStore]
  esNodeStore.clearAll()
  ExternalPropertiesContext.bindCustomPropertyNodeStore(esNodeStore)
  val dbFile: File = new File("testoutput/testdb")
  FileUtils.deleteRecursively(dbFile);
  dbFile.mkdirs();
  val db: GraphDatabaseService = new GraphDatabaseFactory().newEmbeddedDatabaseBuilder(dbFile).newGraphDatabase()
  db.execute("CREATE (n:Person {age: 10, name: 'bob', address: 'CNIC, CAS, Beijing, China'})")
  db.execute("CREATE (n:Person {age: 10, name: 'bob2', address: 'CNIC, CAS, Beijing, China'})")
  db.execute("CREATE (n:Person {age: 40, name: 'alex', address: 'CNIC, CAS, Beijing, China'})")
  db.execute("CREATE (n:Person {age: 40, name: 'alex2', address: 'CNIC, CAS, Beijing, China'})")
  db.execute("CREATE INDEX ON :Person(address)")
  db.execute("CREATE INDEX ON :Person(name)")
  db.execute("CREATE INDEX ON :Person(age)")
  db.execute("CREATE INDEX ON :Person(name, age)")
  db.execute("match (f:Person), (s:Person) where f.age=40 AND s.age=10 CREATE (f)-[hood:Father]->(s)")

  @After
  def shutdownDB(): Unit = {
    db.shutdown()
    neo4jServer.stop()
  }

  def testQuery(query: String, resultKey: String): Unit = {
    val rs = db.execute(query)
    var resultValue: Long = -1
    if (rs.hasNext) {
      resultValue = rs.next().get(resultKey).toString.toLong
      println(resultValue)
    }
    assert(resultValue != -1)
  }

  @Test
  def lessThan(): Unit = {
    testQuery("match (n) where 18>n.age return id(n)", "id(n)")
  }

  @Test
  def greaterThan(): Unit = {
    testQuery("match (n) where 9<n.age return id(n)", "id(n)")
  }

  @Test
  def numberEqual(): Unit = {
    testQuery("match (n) where n.age = 10 return id(n)", "id(n)")
  }

  @Test
  def stringEqual(): Unit = {
    testQuery("match (n) where n.name = 'bob' return count(n)", "count(n)")
  }

  @Test
  def stringEndsWith(): Unit = {
    testQuery("match (n) where n.name ENDS WITH 'b' return count(n)", "count(n)")
  }

  @Test
  def stringStartsWith(): Unit = {
    testQuery("match (n) where n.name STARTS WITH 'b' return id(n)", "id(n)")
  }

  @Test
  def stringEndsWithAnd(): Unit = {
    testQuery("match (n) where n.name ENDS WITH 'b' AND n.address ENDS WITH 'China' AND n.age = 10 return count(n)", "count(n)")
  }

  @Test
  def tripleAnd(): Unit = {
    testQuery("match (n) where n.name ENDS WITH 'b' and n.address ENDS WITH 'China' and n.age = 10 return count(n)", "count(n)")
  }

  @Test
  def andOr(): Unit = {
    testQuery("match (n) where n.name ENDS WITH 'a' OR n.address ENDS WITH 'China' AND n.age = 10 return id(n)", "id(n)")
  }

  @Test
  def tripleOr(): Unit = {
    testQuery("match (n) where n.name ENDS WITH 'a' OR n.address ENDS WITH 'Chinad' OR n.age = 10 return id(n)", "id(n)")
  }

  @Test
  def not(): Unit = {
    testQuery("match (n:Person) where NOT (n.name ENDS WITH 'a') return id(n)", "id(n)")
  }

  @Test
  def label(): Unit = {
    testQuery("match (n:Person) return id(n)", "id(n)")
  }

  @Test
  def labelAndStringEndsWith(): Unit = {
    testQuery("match (n:Person) where n.name ENDS WITH 'b' return id(n)", "id(n)")
  }

  @Test
  def relationStringEndsWith(): Unit = {
    testQuery("match (f:Person {age: 40})-[:Father]->(s:Person) where f.name STARTS WITH 'a' and s.name STARTS WITH 'b' return COUNT(s)", "COUNT(s)")
  }

  @Test
  def indexStringEndsWith(): Unit = {
    testQuery("match (n:Person) USING INDEX n:Person(address, age) where n.address ENDS WITH 'China' and n.age = 10 return id(n)", "id(n)")
  }

  @Test
  def compositeIndexStringEndsWith(): Unit = {
    testQuery("match (n:Person) where n.name = 'bob' and n.age = 10 return count(n)", "count(n)")
  }

  @Test
  def udf(): Unit = {
    testQuery("match (n:Person) where toInteger(n.age) = 10 AND subString(n.address,0,4) = 'CNIC' return id(n)", "id(n)")
  }

  //  @Test
  //  def notEqual(): Unit = {
  //    testQuery("match (f:Person)-[:Father]->(s:Person) where not f.age = s.age return count(f)", "count(f)")
  //  }

  @Test
  def hasProperty(): Unit = {
    testQuery("match (n:Person) WHERE NOT EXISTS (n.age) return count(n)", "count(n)")
  }

  @Test
  def in(): Unit = {
    testQuery("match (n:Person) WHERE n.age IN [40, 10] return count(n)", "count(n)")
  }

}