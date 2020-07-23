package cn.pandadb.jraft

import java.nio.ByteBuffer

import cn.pandadb.jraft.operations.WriteOperations
import com.alipay.remoting.exception.CodecException
import com.alipay.remoting.serialization.SerializerManager
import com.alipay.sofa.jraft.Status
import com.alipay.sofa.jraft.entity.Task
import com.alipay.sofa.jraft.error.RaftError
import org.apache.commons.lang.StringUtils

object PandaJraftService {

  def commitWriteOpeartions(ops: WriteOperations): Unit = {
    val jraftServer: PandaJraftServer = PandaJraftServer.getInstance()

    if (!jraftServer.isLeader) {
      println("not leader")
      return
    }

    try {
//      closure.setCounterOperation(op)
      val task = new Task
      task.setData(ByteBuffer.wrap(SerializerManager.getSerializer(SerializerManager.Hessian2).serialize(ops)))
//      task.setDone(closure)
      jraftServer.getNode.apply(task)
    } catch {
      case e: CodecException =>
        val errorMsg = "Fail to encode CounterOperation"
//        LOG.error(errorMsg, e)
//        closure.failure(errorMsg, StringUtils.EMPTY)
//        closure.run(new Status(RaftError.EINTERNAL, errorMsg))
    }
  }

  def isLeader(): Boolean = {
    val jraftServer: PandaJraftServer = PandaJraftServer.getInstance()
    jraftServer.isLeader
  }
}
