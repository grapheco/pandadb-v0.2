package cn.pandadb.driver

import com.alipay.sofa.jraft.{JRaftUtils, RouteTable}
import com.alipay.sofa.jraft.option.CliOptions
import com.alipay.sofa.jraft.rpc.impl.cli.CliClientServiceImpl
import org.neo4j.driver.{AuthTokens, Driver, GraphDatabase}

import scala.util.Random

object SelectNode {
  val cliClientService = new CliClientServiceImpl
  cliClientService.init(new CliOptions())

  private def initJraft(routeTable: RouteTable, uri: String): Unit = {

    val conf = JRaftUtils.getConfiguration(uri)
    routeTable.updateConfiguration("panda", conf)
    routeTable.refreshConfiguration(cliClientService, "panda", 10000)
  }

  private def getWriteNode(routeTable: RouteTable, uri: String): String = {
    initJraft(routeTable, uri)
    routeTable.refreshLeader(cliClientService, "panda", 10000)
    val leader = routeTable.selectLeader("panda")
    leader.toString
  }

  private def getReadNode(routeTable: RouteTable, uri: String): String = {
    initJraft(routeTable, uri)
    routeTable.refreshLeader(cliClientService, "panda", 10000)
    val peers = routeTable.getConfiguration("panda").getPeers
    val choose = new Random().nextInt(peers.size())
    peers.get(choose).toString
  }

  private def getNode(isWriteStatement: Boolean, routeTable: RouteTable, uri: String): String = {
    if (isWriteStatement) getWriteNode(routeTable, uri) else getReadNode(routeTable, uri)
  }

  def getDriver(isWriteStatement: Boolean, routeTable: RouteTable, uuri: String): Driver = {
    val nn: String = getNode(isWriteStatement, routeTable, uuri)
    val str = nn.split(":")
    val host = str(0)
    val port = str(1)
    //    val uri = s"bolt://$host:$port"
    var uri: String = ""
    uri = port.toInt match {
      case 8081 => s"bolt://$host:7610"
      case 8082 => s"bolt://$host:7620"
      case 8083 => s"bolt://$host:7630"
    }
    GraphDatabase.driver(uri, AuthTokens.basic("neo4j", "neo4j"))
  }
}

