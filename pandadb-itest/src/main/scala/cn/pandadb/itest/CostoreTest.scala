package cn.pandadb.itest

import java.nio.file.Paths
import java.util
import java.util.Optional

import cn.pandadb.config.PandaConfig
import cn.pandadb.costore.InElasticSearchPropertyNodeStore
import cn.pandadb.server.PandaRuntimeContext
import org.neo4j.graphdb.factory.GraphDatabaseFactory
import org.neo4j.server.CommunityBootstrapper
import java.io.File

import cn.pandadb.costore.{CustomPropertyNodeStore, ExternalPropertiesContext}
import org.junit.{After, Before, Test}
import org.neo4j.graphdb.GraphDatabaseService
import org.neo4j.io.fs.FileUtils

class PandaCostoreTest {

  var db: GraphDatabaseService = null
  val confFile: File = new File("testinput/test-costore.conf")
  val dbFile: File = new File("testoutput/testdb")
  FileUtils.deleteRecursively(dbFile);
  dbFile.mkdirs();
  db = new GraphDatabaseFactory().newEmbeddedDatabaseBuilder(dbFile).loadPropertiesFromFile(confFile.getPath).newGraphDatabase()
  println(PandaRuntimeContext.contextGet[PandaConfig]())
  val esNodeStore: InElasticSearchPropertyNodeStore = PandaRuntimeContext.contextGet[CustomPropertyNodeStore]().asInstanceOf[InElasticSearchPropertyNodeStore]
  esNodeStore.clearAll()
  ExternalPropertiesContext.bindCustomPropertyNodeStore(esNodeStore)
  db.execute("CREATE (n:Person {age: 10, name: 'bob', address: 'CNIC, CAS, Beijing, China'})")
  db.execute("CREATE (n:Person {age: 10, name: 'bob2', address: 'CNIC, CAS, Beijing, China'})")
  db.execute("CREATE (n:Person {age: 40, name: 'alex', address: 'CNIC, CAS, Beijing, China'})")
  db.execute("CREATE (n:Person {age: 40, name: 'alex2', address: 'CNIC, CAS, Beijing, China'})")
  db.execute("CREATE INDEX ON :Person(address)")
  db.execute("CREATE INDEX ON :Person(name)")
  db.execute("CREATE INDEX ON :Person(age)")
  db.execute("CREATE INDEX ON :Person(name, age)")
  db.execute("match (f:Person), (s:Person) where f.age=40 AND s.age=10 CREATE (f)-[hood:Father]->(s)")
  println("=================db inintialized================")

  @After
  def shutdownDB(): Unit = {
    db.shutdown()
  }

  def testQuery(query: String, resultKey: String, requiredValue: String = "0"): Unit = {
    val rs = db.execute(query)
    var resultValue: String = "empty results"
    if (rs.hasNext) {
      resultValue = rs.next().get(resultKey).toString
    }
    println(s"resultValue: ${resultValue}, requiredValue: ${requiredValue}")
    assert(resultValue == requiredValue)
  }

  @Test
  def lessThan(): Unit = {
    testQuery("match (n) where 18>n.age return count(n)", "count(n)", "2")
  }

  @Test
  def greaterThan(): Unit = {
    testQuery("match (n) where 9<n.age return count(n)", "count(n)", "4")
  }

  @Test
  def numberEqual(): Unit = {
    testQuery("match (n) where n.age = 10 return count(n)", "count(n)", "2")
  }

  @Test
  def stringEqual(): Unit = {
    testQuery("match (n) where n.name = 'bob' return count(n)", "count(n)", "1")
  }

  @Test
  def stringEndsWith(): Unit = {
    testQuery("match (n) where n.name ENDS WITH 'b' return count(n)", "count(n)", "1")
  }

  @Test
  def stringStartsWith(): Unit = {
    testQuery("match (n) where n.name STARTS WITH 'b' return count(n)", "count(n)", "2")
  }

  @Test
  def stringEndsWithAnd(): Unit = {
    testQuery("match (n) where n.name ENDS WITH 'b' AND n.address ENDS WITH 'China' AND n.age = 10 return count(n)", "count(n)", "1")
  }

  @Test
  def tripleAnd(): Unit = {
    testQuery("match (n) where n.name ENDS WITH 'b' and n.address ENDS WITH 'China' and n.age = 10 return count(n)", "count(n)", "1")
  }

  @Test
  def andOr(): Unit = {
    testQuery("match (n) where n.name ENDS WITH 'a' OR n.address ENDS WITH 'China' AND n.age = 10 return count(n)", "count(n)", "2")
  }

  @Test
  def tripleOr(): Unit = {
    testQuery("match (n) where n.name ENDS WITH 'a' OR n.address ENDS WITH 'Chinad' OR n.age = 11 return count(n)", "count(n)", "0")
  }

  @Test
  def not(): Unit = {
    testQuery("match (n:Person) where NOT (n.name ENDS WITH 'a') return count(n)", "count(n)", "4")
  }

  @Test
  def label(): Unit = {
    testQuery("match (n:Person) return count(n)", "count(n)", "4")
  }

  @Test
  def labelAndStringEndsWith(): Unit = {
    testQuery("match (n:Person) where n.name ENDS WITH 'b' return count(n)", "count(n)", "1")
  }

  @Test
  def relationStringEndsWith(): Unit = {
    testQuery("match (f:Person {age: 40})-[:Father]->(s:Person) where f.name STARTS WITH 'a' and s.name STARTS WITH 'b' return COUNT(s)", "COUNT(s)", "4")
  }

  @Test
  def indexStringEndsWith(): Unit = {
    testQuery("match (n:Person) USING INDEX n:Person(address, age) where n.address ENDS WITH 'China' and n.age = 10 return COUNT(n)", "COUNT(n)", "2")
  }

  @Test
  def compositeIndexStringEndsWith(): Unit = {
    testQuery("match (n:Person) where n.name = 'bob' and n.age = 10 return count(n)", "count(n)", "1")
  }

  @Test
  def udf(): Unit = {
    testQuery("match (n:Person) where toInteger(n.age) = 10 AND subString(n.address,0,4) = 'CNIC' return count(n)", "count(n)", "2")
  }

  //  @Test
  //  def notEqual(): Unit = {
  //    testQuery("match (f:Person)-[:Father]->(s:Person) where not f.age = s.age return count(f)", "count(f)")
  //  }

  @Test
  def hasProperty(): Unit = {
    testQuery("match (n:Person) WHERE NOT EXISTS (n.age) return count(n)", "count(n)", "0")
  }

  @Test
  def in(): Unit = {
    testQuery("match (n:Person) WHERE n.age IN [40, 10] return count(n)", "count(n)", "4")
  }

  @Test
  def basicTypeCheck(): Unit = {
    db.execute("CREATE (n:Person {salary: 40.5, name: 'blue', isLeader: true, address: 'CNIC, CAS, Beijing, China'})")
        testQuery("match (n:Person) WHERE n.isLeader AND n.salary = 40.5 return count(n)", "count(n)", "1")
  }

  @Test
  def pointTypeCheck(): Unit = {
    db.execute("CREATE (n:Person {loc: point({ x:3, y:4 }), salary: 40.5, name: 'blue', isLeader: true, address: 'CNIC, CAS, Beijing, China'})")
    testQuery("match (n:Person) WHERE n.loc.x = 3 AND n.salary = 40.5 return count(n)", "count(n)", "1")
  }

  @Test
  def dateTypeCheck(): Unit = {
    db.execute("CREATE (n:Person {birthday: datetime('1975-06-24T12:50:35.556+0100'), salary: 40.5, name: 'blue', isLeader: true, address: 'CNIC, CAS, Beijing, China'})")
    testQuery("match (n:Person) WHERE n.birthday = datetime('1975-06-24T12:50:35.556+0100') AND n.salary = 40.5 return count(n)", "count(n)", "1")
    db.execute("CREATE (n:Test {timeTest: localtime('12:50:35.556'), name: 'blue'})")
    testQuery("match (n:Test) WHERE n.timeTest = localtime('12:50:35.556') AND n.name = 'blue' return count(n)", "count(n)", "1")
    db.execute("CREATE (n:Test {timeTest: time('125035.556+0100'), name: 'blue'})")
    testQuery("match (n:Test) WHERE n.timeTest = time('125035.556+0100') AND n.name = 'blue' return count(n)", "count(n)", "1")
  }

  @Test
  def ListTypeCheck(): Unit = {
    db.execute("CREATE (n:Test {check: [1997,2003,2003,2000,1999,2003,1995], name: 'blue'})")
    testQuery("match (n:Test) WHERE n.check = [1997,2003,2003,2000,1999,2003,1995] AND n.name = 'blue' return count(n)", "count(n)", "1")
  }

}