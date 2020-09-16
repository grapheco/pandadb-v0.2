package cn.pandadb.jraft.rpc

import cn.pandadb.config.PandaConfig
import cn.pandadb.jraft.{PandaJraftServer, PandaJraftService}
import com.alipay.sofa.jraft.rpc.{RpcContext, RpcProcessor}
import cn.pandadb.server.PandaRuntimeContext


class GetGraphDataStateRequestProcessor(pandaJraftServer: PandaJraftServer) extends RpcProcessor[GetGraphDataStateRequest]{
  override def handleRequest(rpcCtx: RpcContext, request: GetGraphDataStateRequest): Unit = {
    val graphDatabasePathFile = PandaRuntimeContext.contextGet[PandaConfig]().getGraphDatabasePath.toFile
    var response: GraphDataStateResponse = null
    val appliedTxLogIndex = pandaJraftServer.getFsm.logIndexFile.load()
    if (graphDatabasePathFile.exists() && graphDatabasePathFile.isDirectory && graphDatabasePathFile.list().length>0) {
      val files = graphDatabasePathFile.listFiles().filter(file => file.isFile).map(file => file.getName)
      val dirs = graphDatabasePathFile.listFiles().filter(file => file.isDirectory).map(file => file.getName)

      response = new GraphDataStateResponse(false, files, dirs, appliedTxLogIndex)
    } else {
      response = new GraphDataStateResponse(true, null, null, appliedTxLogIndex)
    }
    rpcCtx.sendResponse(response)
  }

  override def interest(): String = {
    classOf[GetGraphDataStateRequest].getName
  }
}
