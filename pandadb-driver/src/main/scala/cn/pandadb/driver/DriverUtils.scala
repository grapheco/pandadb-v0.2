package cn.pandadb.driver

import java.util.Locale

import cn.pandadb.driver.SelectNode.cliClientService
import cn.pandadb.jraft.rpc.GetNeo4jBoltAddressRequest
import com.alipay.sofa.jraft.entity.PeerId

object DriverUtils {

  def isWriteStatement(cypherStr: String): Boolean = {
    val lowerCypher = cypherStr.toLowerCase(Locale.ROOT)
    if (lowerCypher.contains("explain")) {
      false
    } else if (lowerCypher.contains("create") || lowerCypher.contains("merge") ||
      lowerCypher.contains("set") || lowerCypher.contains("delete")) {
      true
    } else {
      false
    }
  }

  def getBoltPort(peerIp: String, peerPort: Int): String = {
    val request = new GetNeo4jBoltAddressRequest
    val client = cliClientService.getRpcClient
    val peer = new PeerId(peerIp, peerPort.toInt)
    var boltPort: Any = null
    val res = client.invokeSync(peer.getEndpoint, request, 5000)
    boltPort = res.toString
    val port = boltPort.toString.split(":")(1)
    port
  }
}
