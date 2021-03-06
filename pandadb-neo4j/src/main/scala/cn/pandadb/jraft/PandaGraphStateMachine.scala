package cn.pandadb.jraft

import java.io.{File, IOException}
import java.nio.ByteBuffer
import java.nio.file.Paths
import java.util.concurrent.atomic.AtomicLong

import cn.pandadb.config.PandaConfig

import scala.collection.JavaConversions._
import com.alipay.remoting.exception.CodecException
import com.alipay.remoting.serialization.SerializerManager
import com.alipay.sofa.jraft.{Closure, Status, Iterator => SofaIterator}
import com.alipay.sofa.jraft.core.StateMachineAdapter
import com.alipay.sofa.jraft.error.{RaftError, RaftException}
import com.alipay.sofa.jraft.storage.snapshot.{SnapshotReader, SnapshotWriter}
import com.alipay.sofa.jraft.util.Utils
import org.neo4j.graphdb.GraphDatabaseService
import cn.pandadb.jraft.operations.WriteOperations
import cn.pandadb.jraft.snapshot.{LogIndexFile, PandaGraphSnapshotFile}
import cn.pandadb.server.{Logging, PandaRuntimeContext}


class PandaGraphStateMachine() extends StateMachineAdapter with Logging{

  /**
    * Leader term
    */
  private val leaderTerm = new AtomicLong(-1)

  val logIndexFile: LogIndexFile = new LogIndexFile(getLogIndexPath())

  def isLeader: Boolean = this.leaderTerm.get > 0

  def getLogIndexPath(): String = {
    Paths.get(getDataPath(), "logIndex").toString
  }

  override def onApply(iter: SofaIterator): Unit = {
    PandaRuntimeContext.setSnapshotLoaded(true)
    while (PandaRuntimeContext.contextGetOption[GraphDatabaseService]().isEmpty) {
      logger.info("wait for GraphDatabaseService created")
      Thread.sleep(500)
    }
    val neo4jDB = PandaRuntimeContext.contextGet[GraphDatabaseService]()
    while (!neo4jDB.isAvailable(1000)) {
      logger.info("wait for GraphDatabaseService available")
    }
    var logIndex: Long = logIndexFile.load()
    while ( iter.hasNext()) {
      var writeOperations: WriteOperations = null
      // leader not apply task
      if (!isLeader && iter.getIndex > logIndex) { // Have to parse FetchAddRequest from this user log.
        val data = iter.getData
        try {
          writeOperations = SerializerManager.getSerializer(SerializerManager.Hessian2).deserialize(data.array, classOf[WriteOperations].getName)
        }
        catch {
          case e: CodecException =>
            logger.error("Fail to decode WriteOperations", e)
        }
        if (writeOperations != null) {
          writeOperations.applyTxOpeartionsToDB(neo4jDB)
        }
      }
      iter.next
    }
    val logIndexNew = iter.getIndex.toInt - 1
    if (logIndexNew > logIndex) logIndex = logIndexNew
    logIndexFile.save(logIndex)
  }

  def getDataPath(): String = {
    val pandaConfig: PandaConfig = PandaRuntimeContext.contextGet[PandaConfig]()
    pandaConfig.dataPath
  }

  def getActiveDatabase(): String = {
    val pandaConfig: PandaConfig = PandaRuntimeContext.contextGet[PandaConfig]()
    pandaConfig.activeDatabase
  }
  override def onSnapshotSave(writer: SnapshotWriter, done: Closure): Unit = {
    logger.info("save snapshot.")
    val snap = new PandaGraphSnapshotFile
    Utils.runInThread(new Runnable {
      override def run(): Unit = {
        val dataPathFile = Paths.get(getDataPath(), "databases" + File.separator + getActiveDatabase).toFile
        if (dataPathFile.exists()) {
          val dataPath = dataPathFile.getAbsoluteFile.toString
          snap.save(dataPath, writer.getPath)
          if (writer.addFile("backup.zip")) done.run(Status.OK())
        }
      }
    })
  }

  override def onError(e: RaftException): Unit = {
    logger.error("Raft error: {}", e)
  }

  override def onSnapshotLoad(reader: SnapshotReader): Boolean = {
    if (isLeader) {
      logger.warn("Leader is not supposed to load snapshot")
      return false
    }
    if (reader.getFileMeta("backup.zip") == null) {
      logger.error("Fail to find data file in {}", reader.getPath)
      return false
    }
    logger.info("load snapshot.")
    val snap = new PandaGraphSnapshotFile
    val loadDirectory = new File(reader.getPath)
    val dataPath = Paths.get(getDataPath(), "databases").toString
    var ret = false
    if (loadDirectory.isDirectory) {
      val files = loadDirectory.listFiles()
      if (files.length == 0) {
        logger.error("snapshot file is not existed.")
      }
      else {
        files.foreach(f => {
          if (f.getName.endsWith("zip")) snap.load(f.getAbsolutePath, dataPath)
        })
        ret = true
      }
    }
    else {
      logger.error("snapshot file save directory is not existed.")
    }
    PandaRuntimeContext.setSnapshotLoaded(true)
    ret
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
