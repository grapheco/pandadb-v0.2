package org.neo4j.kernel.impl.blob

import java.io.File

import org.apache.hadoop.hbase.client.ColumnFamilyDescriptorBuilder.ModifyableColumnFamilyDescriptor
import org.apache.hadoop.hbase.client.TableDescriptorBuilder.ModifyableTableDescriptor
import org.apache.hadoop.hbase.client._
import org.apache.hadoop.hbase.{HBaseConfiguration, TableName}
import org.neo4j.blob.util.{Configuration, Logging}
import org.neo4j.blob.{Blob, BlobId}

import scala.collection.JavaConversions._
import scala.concurrent.forkjoin.ForkJoinPool

class HBaseBlobValueStorage extends BlobStorage with Logging {
  private var _table: Table = _
  private var conn: Connection = _
  private var _hbaseUtil: HBaseUtils = _

  override def initialize(storeDir: File, conf: Configuration): Unit = {
    val hbaseConf = HBaseConfiguration.create()
    val zkQ = conf.getRaw("blob.storage.hbase.zookeeper.quorum").getOrElse("localhost")
    val zkNode = conf.getRaw("blob.storage.hbase.zookeeper.znode.parent").getOrElse("/hbase")
    logger.info(s"zk:$zkQ, zkNode:$zkNode")
    hbaseConf.set("hbase.zookeeper.quorum", zkQ)
    hbaseConf.set("zookeeper.znode.parent", zkNode)
    HBaseAdmin.available(hbaseConf)

    _hbaseUtil = new HBaseUtils()

    logger.info("successfully initial the connection to the zookeeper")
    // get HTable to save blob
    val tableNameStr = conf.getRaw("blob.storage.hbase.table").getOrElse("BlobTable")
    val tableName = TableName.valueOf(tableNameStr)
    conn = ConnectionFactory.createConnection(hbaseConf)
    val admin = conn.getAdmin
    if (!admin.tableExists(tableName)) {
      if (conf.getRaw("blob.storage.hbase.auto_create_table").getOrElse("false").toBoolean) {
        admin.createTable(new ModifyableTableDescriptor(tableName)
          .setColumnFamily(new ModifyableColumnFamilyDescriptor(_hbaseUtil.columnFamily)))
        logger.info(s"table created: $tableName")
      }
      else {
        logger.error(s"table is not existed: $tableName")
        throw new Exception("Error: Table is not existed.")
      }

    }
    _table = conn.getTable(tableName, ForkJoinPool.commonPool())
    if (_table.getDescriptor().getColumnFamily(_hbaseUtil.columnFamily) == null) {
      admin.addColumnFamily(tableName, new ModifyableColumnFamilyDescriptor(_hbaseUtil.columnFamily))
    }
    logger.info(s"table is gotten : $tableName")
  }

  override def disconnect(): Unit = {
    _table.close()
    conn.close()
    logger.info(s"HBase connect is closed")
  }

  private def generateId(): BlobId = {
    _hbaseUtil.generateId()
  }

  override def save(blob: Blob): BlobId = {
    val bid = generateId()
    _table.put(_hbaseUtil.buildPut(blob, bid))
    logger.debug(s"saved blob: ${bid.asLiteralString()}")
    bid;
  }

  override def load(id: BlobId): Option[Blob] = {
    val res = _table.get(_hbaseUtil.buildBlobGet(id))
    if (!res.isEmpty) {
      val blob = _hbaseUtil.buildBlobFromGetResult(res).head._2
      logger.debug(s"loaded blob: ${id.asLiteralString()}")
      Some(blob)
    }
    else None
  }

  override def delete(id: BlobId): Unit = {
    _table.delete(_hbaseUtil.buildDelete(id))
    logger.debug(s"deleted blob: ${id.asLiteralString()}");
  }

  override def loadGroup(gid: BlobId): Option[Array[Blob]] = {
    val res = _table.get(_hbaseUtil.buildBlobGetGroup(gid))
    if (!res.isEmpty) {
      val array = _hbaseUtil.buildBlobGroupFromGetResult(res)
      Some(array)
    }
    else null
  }

  override def saveGroup(blobs: Array[Blob]): BlobId = {
    val gid = generateId()
    for (i <- blobs.indices) {
      _table.put(_hbaseUtil.buildPutGroup(blobs(i), gid, i))
    }
    gid
  }

  override def deleteGroup(gid: BlobId): Unit = {
    _table.delete(_hbaseUtil.buildDeleteGroup(gid))
  }

  override def existsGroup(gid: BlobId): Boolean = {
    val res = _table.get(_hbaseUtil.buildBlobGetGroup(gid))
    !res.isEmpty
  }

  override def exists(bid: BlobId): Boolean = {
    val res = _table.get(_hbaseUtil.buildBlobGet(bid))
    !res.isEmpty
  }
}


