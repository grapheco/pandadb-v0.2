package cn.pandadb.driver.v2

import cn.pandadb.jraft.rpc.GetNeo4jBoltAddressRequest
import com.alipay.sofa.jraft.entity.PeerId
import com.alipay.sofa.jraft.option.CliOptions
import com.alipay.sofa.jraft.rpc.impl.cli.CliClientServiceImpl
import com.alipay.sofa.jraft.{JRaftUtils, RouteTable}
import org.neo4j.driver.{AuthTokens, Driver, GraphDatabase, Session}

import scala.util.Random

class PandaDriver(uri: String, username: String, password: String) {

  if (!uri.startsWith("panda://") && !uri.startsWith("bolt://")) {
    throw new IllegalArgumentException("Uri Error")
  }

  private val cliClientService = new CliClientServiceImpl
  private val groupId = "panda"
  private val rt = RouteTable.getInstance()
  cliClientService.init(new CliOptions())

  private val conf = JRaftUtils.getConfiguration(uri.split("//")(1))
  private var driver: Driver = _


  private def getBoltPort(peerIp: String, peerPort: Int): String = {
    val request = new GetNeo4jBoltAddressRequest
    val client = cliClientService.getRpcClient
    val peer = new PeerId(peerIp, peerPort.toInt)
    val res = client.invokeSync(peer.getEndpoint, request, 5000)
    val boltPort = res.toString.split(":")(1)
    if (boltPort.contains("}")) {
      boltPort.split("}")(0)
    } else {
      boltPort
    }
  }

  private def initJraft(): Unit = {
    rt.updateConfiguration(groupId, conf)
    rt.refreshConfiguration(cliClientService, groupId, 10000)
    rt.refreshLeader(cliClientService, groupId, 10000)
    val leader = rt.selectLeader(groupId)
    if (leader == null) throw new IllegalArgumentException("check your jraft server port ")
  }

  def writeSession(): Session = {
    if (uri.startsWith("panda://")) {
      initJraft()
      rt.refreshLeader(cliClientService, groupId, 10000)
      val strs = rt.selectLeader(groupId).toString.split(":")
      val bolt = getBoltPort(strs(0), strs(1).toInt)
      val leaderUri = s"bolt://${strs(0)}:$bolt"
      try {
        driver = GraphDatabase.driver(leaderUri, AuthTokens.basic(username, password))
      } catch {
        case exception: Exception => throw new IllegalArgumentException("check your jraft server port ")
      }
    } else {
      try {
        driver = GraphDatabase.driver(uri, AuthTokens.basic(username, password))
      } catch {
        case exception: Exception => {
          println(exception)
          throw new IllegalArgumentException("check your bolt server port ")
        }
      }
    }
    driver.session()
  }

  def readSession(): Session = {
    if (uri.startsWith("panda://")) {
      initJraft()
      val peers = rt.getConfiguration(groupId).getPeers
      val choose = new Random().nextInt(peers.size())
      val str = peers.get(choose).toString.split(":")
      val boltPort = getBoltPort(str(0), str(1).toInt)
      val rUri = s"bolt://${str(0)}:$boltPort"

      try {
        driver = GraphDatabase.driver(rUri, AuthTokens.basic(username, password))
      } catch {
        case exception: Exception => throw new IllegalArgumentException("check your jraft server port ")
      }
    } else {
      try {
        driver = GraphDatabase.driver(uri, AuthTokens.basic(username, password))
      } catch {
        case exception: Exception => throw new IllegalArgumentException("check your bolt server port ")
      }
    }
    driver.session()
  }

  def close(): Unit = {
    driver.close()
  }

}
