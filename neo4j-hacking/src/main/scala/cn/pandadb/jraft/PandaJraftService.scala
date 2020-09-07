package cn.pandadb.jraft

import java.nio.ByteBuffer

import cn.pandadb.config.PandaConfig
import cn.pandadb.jraft.operations.WriteOperations
import cn.pandadb.server.{Logging, PandaRuntimeContext}
import com.alipay.remoting.exception.CodecException
import com.alipay.remoting.serialization.SerializerManager
import com.alipay.sofa.jraft.entity.{PeerId, Task}
import org.neo4j.graphdb.GraphDatabaseService
import org.neo4j.kernel.lifecycle.Lifecycle


class PandaJraftService(neo4jDB: GraphDatabaseService) extends Lifecycle with Logging {
  var jraftServer: PandaJraftServer = null
  val pandaConfig: PandaConfig = PandaRuntimeContext.contextGet[PandaConfig]()

  PandaRuntimeContext.contextPut[PandaJraftService](this)

  def commitWriteOpeartions(ops: WriteOperations): Unit = {
    if (!jraftServer.isLeader || ops.size == 0) {
      return
    }

    try {
      val task = new Task
      task.setData(ByteBuffer.wrap(SerializerManager.getSerializer(SerializerManager.Hessian2).serialize(ops)))
      jraftServer.getNode.apply(task)
    } catch {
      case e: CodecException =>
        val errorMsg = "Fail to encode tx operation"
        logger.error(errorMsg, e)
    }
  }

  def isLeader(): Boolean = {
    jraftServer.isLeader
  }

  override def init(): Unit = {
    val dataPath: String = pandaConfig.jraftDataPath
    val serverId: String = pandaConfig.jraftServerId
    val groupId: String = pandaConfig.jraftGroupId
    val peers: String = pandaConfig.jraftPeerIds
    jraftServer = new PandaJraftServer(neo4jDB, dataPath, groupId, serverId, peers)
    logger.info("==== jraft server init ====")
  }

  override def start(): Unit = {
    jraftServer.start()
    logger.info("==== jraft server started ====")
  }

  override def stop(): Unit = {
    logger.info("==== jraft server stop ====")
    shutdown()
  }

  override def shutdown(): Unit = {
    jraftServer.shutdown()
    logger.info("==== jraft server shutdown ====")
  }

  def getPeers(): Set[PeerId] = {
    jraftServer.getPeers()
  }

  def getLeader(): PeerId = {
    jraftServer.getLeader()
  }
}
