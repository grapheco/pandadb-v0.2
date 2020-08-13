package cn.pandadb.jraft

import java.io.{File, IOException}
import java.nio.ByteBuffer
import java.util.concurrent.atomic.AtomicLong

import scala.collection.JavaConversions._
import com.alipay.remoting.exception.CodecException
import com.alipay.remoting.serialization.SerializerManager
import com.alipay.sofa.jraft.{Closure, Status, Iterator => SofaIterator}
import com.alipay.sofa.jraft.core.StateMachineAdapter
import com.alipay.sofa.jraft.error.{RaftError, RaftException}
import com.alipay.sofa.jraft.storage.snapshot.{SnapshotReader, SnapshotWriter}
import com.alipay.sofa.jraft.util.Utils
import org.slf4j.{Logger, LoggerFactory}
import org.neo4j.graphdb.GraphDatabaseService
import cn.pandadb.jraft.operations.WriteOperations
import cn.pandadb.jraft.snapshot.PandaGraphSnapshotFile


class PandaGraphStateMachine(val neo4jDB: GraphDatabaseService) extends StateMachineAdapter {
  private val LOG = LoggerFactory.getLogger(classOf[PandaGraphStateMachine])

  /**
    * Leader term
    */
  private val leaderTerm = new AtomicLong(-1)

  def isLeader: Boolean = this.leaderTerm.get > 0

  override def onApply(iter: SofaIterator): Unit = {
    while ( iter.hasNext() ) {
      var writeOperations: WriteOperations = null
//      var closure = null
//      if (iter.done != null) { // This task is applied by this node, get value from closure to avoid additional parsing.
//        closure = iter.done.asInstanceOf[CounterClosure]
//        counterOperation = closure.getCounterOperation
//      }
      // leader not apply task
      if (!isLeader) { // Have to parse FetchAddRequest from this user log.
        val data = iter.getData
        try
          writeOperations = SerializerManager.getSerializer(SerializerManager.Hessian2).deserialize(data.array, classOf[WriteOperations].getName)
        catch {
          case e: CodecException =>
            LOG.error("Fail to decode IncrementAndGetRequest", e)
        }
        if (writeOperations != null) {
          writeOperations.applyTxOpeartionsToDB(neo4jDB)

  //        if (closure != null) {
  //          closure.success(current)
  //          closure.run(Status.OK)
  //        }
        }
      }
      iter.next
    }
  }

  override def onSnapshotSave(writer: SnapshotWriter, done: Closure): Unit = {
    println("snopshot================777777")
    val snap = new PandaGraphSnapshotFile
    Utils.runInThread(new Runnable {
      override def run(): Unit = {
        var dataPath = writer.getPath.substring(0, 20)
        dataPath = dataPath.concat("data\\databases\\graph.db\\")
        val psnapPath = writer.getPath.substring(0, 20).concat("snapshot")
        snap.save(dataPath, writer.getPath)
        //snap.save(dataPath, psnapPath)
        println("snopshot================5555" + writer.getPath)
        println("snopshot================66666" + dataPath)
        //if (writer.addFile("data"))
        done.run(Status.OK())
      }
    })
//    val dbFilePath = null
//    Utils.runInThread(() => {
//      def foo() = {
//        val snapshot = new CounterSnapshotFile(writer.getPath + File.separator + "data")
//        if (snapshot.save(currVal)) if (writer.addFile("data")) done.run(Status.OK)
//        else done.run(new Status(RaftError.EIO, "Fail to add file to writer"))
//        else done.run(new Status(RaftError.EIO, "Fail to save counter snapshot %s", snapshot.getPath))
//      }
//
//      foo()
//    })
  }

  override def onError(e: RaftException): Unit = {
//    LOG.error("Raft error: {}", e, e)
    println("Raft error: " + e )
  }

  override def onSnapshotLoad(reader: SnapshotReader): Boolean = {
    println("============load")
    println("=============" + reader.getPath)
    val snap = new PandaGraphSnapshotFile
    val loadDirectory = new File(reader.getPath)
    if (loadDirectory.isDirectory) {
      val files = loadDirectory.listFiles()
      files.foreach(f => {
        if (f.getName.endsWith("zip")) snap.load(f.getAbsolutePath, reader.getPath.substring(0, 20).concat("data\\databases\\"))
      })
    }
    else {
      println("snapshot file not exist")
    }
//    if (isLeader) {
//      LOG.warn("Leader is not supposed to load snapshot")
//      return false
//    }
//    if (reader.getFileMeta("data") == null) {
//      LOG.error("Fail to find data file in {}", reader.getPath)
//      return false
//    }
//    val snapshot = new CounterSnapshotFile(reader.getPath + File.separator + "data")
//    try {
//      this.value.set(snapshot.load)
//      true
//    } catch {
//      case e: IOException =>
//        LOG.error("Fail to load snapshot from {}", snapshot.getPath)
//        false
//    }
    true
  }

  override def onLeaderStart(term: Long): Unit = {
    this.leaderTerm.set(term)
    super.onLeaderStart(term)
  }

  override def onLeaderStop(status: Status): Unit = {
    this.leaderTerm.set(-1)
    super.onLeaderStop(status)
  }
}
