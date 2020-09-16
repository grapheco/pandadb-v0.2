package cn.pandadb.jraft.rpc

import cn.pandadb.jraft.PandaJraftServer
import com.alipay.sofa.jraft.rpc.{RpcContext, RpcProcessor}

class GetNeo4jBoltAddressRequestProcessor(pandaJraftServer: PandaJraftServer) extends RpcProcessor[cn.pandadb.jraft.rpc.GetNeo4jBoltAddressRequest]{
  override def handleRequest(rpcCtx: RpcContext, request: GetNeo4jBoltAddressRequest): Unit = {
    rpcCtx.sendResponse(new Neo4jBoltAddressValue(pandaJraftServer.getNeo4jBoltServerAddress()))
  }

  override def interest(): String = {
    classOf[GetNeo4jBoltAddressRequest].getName
  }
}
