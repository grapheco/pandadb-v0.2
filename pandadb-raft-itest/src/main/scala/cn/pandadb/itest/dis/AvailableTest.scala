package cn.pandadb.itest.dis

import java.io.File
import java.nio.file.Paths
import java.text.SimpleDateFormat
import java.util.Date
import java.util.concurrent.atomic.{AtomicInteger, AtomicLong}

import org.junit.{After, Before, Test}
import org.neo4j.driver.{AuthTokens, GraphDatabase}

import scala.util.Random
class Generator {
  val operationsArray = Array("w", "r", "r", "w", "w", "w", "r", "r")
  private val cntId = new AtomicInteger(0)
  def produceOperation(): String = {
    var cypherString: String = null
    val op = operationsArray(Random.nextInt(operationsArray.size))
    if (op=="w") {
      cypherString = s"match(n:test) set n.name = ${cntId.addAndGet(1).toString})"
    }
    else {
      cypherString = s"match(n:test) return n"
    }
    cypherString
  }
}
class Client(bolt: String, generator: Generator) {
  //val driver = GraphDatabase.driver(bolt, AuthTokens.basic("neo4j", "neo4j"))
  //val session = driver.session()
  var exit: Boolean = false
  def startTest(): Unit = {
      val thread = new Thread(new Runnable {
        override def run(): Unit = {
          while (!exit) {

            val time1 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date)
            val cypher = generator.produceOperation()
            Thread.sleep(1000)
            val time2 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date)
            println(s"client ${bolt} run ${cypher} at : ${time1} ------ ${time2}")
          }
        }
      })
    thread.start()
    //Thread.sleep(10000)
    //this.exit = true
  }
}

class Destroy(sp: ServerBootStrap, nodeInfoArray: Array[NodeInfo]) {
  var exit: Boolean = false
  def startDestroy(): Unit = {
    val thread = new Thread(new Runnable {
      override def run(): Unit = {
        while (!exit) {
          if (nodeInfoArray.map(_.isStart).reduce(_&_)) stopOneNode
          else {
            startOneNode()
            Thread.sleep(5000)
          }
        }
      }
    })
    thread.start()
    Thread.sleep(20000)
    this.exit = true
  }

  def stopOneNode(): Unit = {
    sp.stopNode(nodeInfoArray(Random.nextInt(nodeInfoArray.size)))
  }

  def startOneNode(): Unit = {
    nodeInfoArray.foreach(u => {
      if (u.isStart == false) sp.startNode(u)
    })
  }

}
class AvailableTest {

  final val pathsr = "F:\\IdCode\\pandadb-v0.2\\pandadb-raft-itest\\testoutput"
  @Before
  def beforeTest(): Unit = {
    val path = Paths.get(pathsr)
    val file = path.toFile
    if (file.exists()) delDir(path.toFile)

  }
  @After
  def afterTest(): Unit = {
    Thread.sleep(5000)
    val path = Paths.get(pathsr)
    val file = path.toFile
    if (file.exists()) delDir(path.toFile)
  }
  def delDir(dir: File): Unit = {
    dir.listFiles().foreach(file => {
      if (file.isDirectory) {
        delDir(file)
      }
      else {
        //println(file.getName)
        file.delete()
      }
    })
    dir.delete()
  }

  def startThreeNodes(sp: ServerBootStrap): Array[NodeInfo] = {
    val node1 = new NodeInfo("./testinput/test1.conf", "./testoutput", "data1")
    val node2 = new NodeInfo("./testinput/test2.conf", "./testoutput", "data2")
    val node3 = new NodeInfo("./testinput/test3.conf", "./testoutput", "data3")
    sp.startNode(node1)
    sp.startNode(node2)
    sp.startNode(node3)
    Array(node1, node2, node3)
  }

  @Test
  def testClient(): Unit = {
    val g = new Generator
    val c1 = new Client("1", g)
    c1.startTest()
    Thread.sleep(10000)
    c1.exit = true
    val sp = new ServerBootStrap
    val nodes = startThreeNodes(sp)
    Thread.sleep(10000)
    val des = new Destroy(sp, nodes)
    des.startDestroy()
    sp.stopAllnodes()
  }
}
