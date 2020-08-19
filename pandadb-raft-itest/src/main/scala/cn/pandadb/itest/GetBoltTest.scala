package cn.pandadb.itest

import cn.pandadb.jraft.rpc.{Neo4jBoltAddressValue, GetNeo4jBoltAddressRequest}
import com.alipay.sofa.jraft.RouteTable
import com.alipay.sofa.jraft.conf.Configuration
import com.alipay.sofa.jraft.entity.PeerId
import com.alipay.sofa.jraft.option.CliOptions
import com.alipay.sofa.jraft.rpc.InvokeCallback
import com.alipay.sofa.jraft.rpc.impl.cli.CliClientServiceImpl
import org.junit.{Before, Test}
// scalastyle:off println println(...)
class GetBoltTest {
  var groupId: String = "panda"
  var confstr: String = "127.0.0.1:8081,127.0.0.1:8082,127.0.0.1:8083,127.0.0.1:8084,127.0.0.1:8085"
  var conf: Configuration = new Configuration()

  if (!conf.parse(confstr)) {
    println("Failt to parse conf: " + confstr)
  }

  val cli = new CliClientServiceImpl
  cli.init(new CliOptions)
  RouteTable.getInstance().updateConfiguration(groupId, conf)
  if (!RouteTable.getInstance().refreshLeader(cli, groupId, 1000).isOk) {
    println("Refresh leader failed")
  }
  val leader = RouteTable.getInstance().selectLeader(groupId)
  //println("leader is at : " + leader.toString)

  @Test
  def getBoltTest(): Unit = {
    val request = new GetNeo4jBoltAddressRequest
    val client = cli.getRpcClient
    //val peer = new PeerId()
    //peer.parse("localhost:8085")

    //println(client.(leader.getEndpoint))
    var str: Any = null
    client.invokeAsync(leader.getEndpoint, request, new InvokeCallback {
      override def complete(result: Any, err: Throwable): Unit = {
        if (err == null) {
          str = result
          //println("getNeo4jBoltServerAddress: " + result)
        }
        else err.printStackTrace()
      }
    }, 500)
    println("get bolt hahaha: " + str)
    //println(cli.getRpcClient.checkConnection(leader.getEndpoint))
    //cli.getRpcClient.invokeSync(leader.getEndpoint, request, 1000)
  }
}
