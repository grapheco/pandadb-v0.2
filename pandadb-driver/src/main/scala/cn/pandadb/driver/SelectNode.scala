package cn.pandadb.driver

import com.alipay.sofa.jraft.{JRaftUtils, RouteTable}
import com.alipay.sofa.jraft.option.CliOptions
import com.alipay.sofa.jraft.rpc.impl.cli.CliClientServiceImpl
import org.neo4j.driver.{AuthToken, Driver, GraphDatabase}

import scala.util.Random

object SelectNode {
  val cliClientService = new CliClientServiceImpl
  cliClientService.init(new CliOptions())
  val groupId = "panda"

  private def initJraft(routeTable: RouteTable, uri: String): Unit = {
    val conf = JRaftUtils.getConfiguration(uri)
    routeTable.updateConfiguration(groupId, conf)
    routeTable.refreshConfiguration(cliClientService, groupId, 10000)
  }

  private def getWriteNode(routeTable: RouteTable, uri: String): String = {
    initJraft(routeTable, uri)
    routeTable.refreshLeader(cliClientService, groupId, 10000)
    val leader = routeTable.selectLeader(groupId)
    leader.toString
  }

  private def getReadNode(routeTable: RouteTable, uri: String): String = {
    initJraft(routeTable, uri)
    routeTable.refreshLeader(cliClientService, groupId, 10000)
    val peers = routeTable.getConfiguration(groupId).getPeers
    val choose = new Random().nextInt(peers.size())
    peers.get(choose).toString
  }

  private def getNode(isWriteStatement: Boolean, routeTable: RouteTable, uri: String): String = {
    if (isWriteStatement) getWriteNode(routeTable, uri) else getReadNode(routeTable, uri)
  }

  def getDriver(authTokens: AuthToken, isWriteStatement: Boolean, routeTable: RouteTable, uuri: String): Driver = {
    initJraft(routeTable, uuri)
    val leader = routeTable.selectLeader(groupId)
    if (leader != null) {
      val nn: String = getNode(isWriteStatement, routeTable, uuri)
      val str = nn.split(":")
      val peerIp = str(0) // peer ip
      val peerPort = str(1) // peer port
      //get bolt port
      val boltPort = DriverUtils.getBoltPort(peerIp, peerPort.toInt)
      val uri: String = s"bolt://$peerIp:$boltPort"
      GraphDatabase.driver(uri, authTokens)
    }
    else {
      try {
        val uri: String = s"bolt://$uuri"
        GraphDatabase.driver(uri, authTokens)
      } catch {
        case exception: Exception => throw new IllegalArgumentException("if not cluster model, port should be bolt port!")
      }
    }
  }
}

