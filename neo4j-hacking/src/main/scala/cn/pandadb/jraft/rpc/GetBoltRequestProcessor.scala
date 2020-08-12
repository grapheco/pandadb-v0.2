package cn.pandadb.jraft.rpc

import cn.pandadb.jraft.PandaJraftServer
import com.alipay.sofa.jraft.rpc.{RpcContext, RpcProcessor}

class GetBoltRequestProcessor(pandaJraftServer: PandaJraftServer) extends RpcProcessor[cn.pandadb.jraft.rpc.GetBoltRequest]{
  override def handleRequest(rpcCtx: RpcContext, request: GetBoltRequest): Unit = {
    println("hello world!!!!!")
    rpcCtx.sendResponse(new BoltValue(pandaJraftServer.getBolt()))
  }

  override def interest(): String = {
    val request = new GetBoltRequest
    request.getClass.getName
  }
}
