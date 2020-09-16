package cn.pandadb.jraft

import cn.pandadb.jraft.rpc.{GetGraphDataStateRequest, GraphDataStateResponse}
import cn.pandadb.server.Logging
import com.alipay.sofa.jraft.RouteTable
import com.alipay.sofa.jraft.conf.Configuration
import com.alipay.sofa.jraft.entity.PeerId
import com.alipay.sofa.jraft.option.CliOptions
import com.alipay.sofa.jraft.rpc.RpcClient
import com.alipay.sofa.jraft.rpc.impl.cli.CliClientServiceImpl
import cn.pandadb.server.PandaRuntimeContext

object PandaJraftClient extends Logging{

  def getOrCreateRpcClient(): RpcClient = {
    if (PandaRuntimeContext.contextGetOption[RpcClient]().isEmpty) {
      val cliClientService = new CliClientServiceImpl
      cliClientService.init(new CliOptions)
      val rpcClient = cliClientService.getRpcClient
      PandaRuntimeContext.contextPut[RpcClient](rpcClient)
    }
    PandaRuntimeContext.contextGet[RpcClient]()
  }

  def getRemoteGraphDataState(peerId: PeerId) : GraphDataStateResponse = {
    val request = new GetGraphDataStateRequest
    getOrCreateRpcClient().invokeSync(peerId.getEndpoint, request, 5000)
                          .asInstanceOf[GraphDataStateResponse]
  }

}
