package org.neo4j.kernel.impl.blob

import java.io.{ByteArrayInputStream, InputStream}
import java.util
import java.util.{Properties, UUID}

import org.apache.hadoop.hbase.client._
import org.apache.hadoop.hbase.util.Bytes
import org.neo4j.blob.impl.BlobFactory
import org.neo4j.blob.util.{Logging, StreamUtils}
import org.neo4j.blob.{Blob, BlobId, InputStreamSource, MimeType}

import scala.collection.JavaConversions._
import scala.collection.mutable.ArrayBuffer

class HBaseUtils(columnCount: Int = 1024) extends Logging {
  val colCount: Int = columnCount
  // have fixed 16 bytes array
  val colSize: Int = 2 // cut last 2 bytes as columnName, 2 ^ (8 * 2) > 1024

  val columnFamily: Array[Byte] = Bytes.toBytes("BLOB")

  val properties = new Properties();
  properties.load(this.getClass.getClassLoader.getResourceAsStream("mime.properties"));
  val code2Types = properties.map(x => (x._1.toLong, x._2.toLowerCase())).toMap

  def generateId(): BlobId = {
    val uuid = UUID.randomUUID()
    new BlobId(uuid.getMostSignificantBits, uuid.getLeastSignificantBits)
  }

  // map BlobId to RowKey and ColName of HTable
  def blobId2RowCol(blobId: BlobId): (Array[Byte], Array[Byte]) = {
    if (colSize == 0) {
      (blobId.asByteArray(), Bytes.toBytes(0))
    } else {
      val bidBytes = blobId.asByteArray()
      (bidBytes.take(bidBytes.length - colSize), bidBytes.takeRight(colSize))
    }
  }

  def rowCol2BlobId(row: Array[Byte], col: Array[Byte]): BlobId = {
    val bidBytes: Array[Byte] = Array.concat(row, col)
    BlobId.fromBytes(bidBytes)
  }

  // merge [blob.mimeType.code, blob.length, blob.toBytes] and save to one cell
  def blobToCellData(blob: Blob): Array[Byte] = {
    Array.concat(Bytes.toBytes(blob.mimeType.code), Bytes.toBytes(blob.length), blob.toBytes())
  }

  def cellDataToBlob(cellData: Array[Byte]): Blob = {
    val mimeTypeCode = StreamUtils.convertByteArray2LongArray(cellData.take(8))
    val len = StreamUtils.convertByteArray2LongArray(cellData.slice(8, 16))

    val in = new ByteArrayInputStream(cellData.slice(16, cellData.length))

    val blob = BlobFactory.fromInputStreamSource(new InputStreamSource {
      /**
       * note close input stream after consuming
       */
      override def offerStream[T](consume: InputStream => T): T = {
        val t = consume(in)
        in.close()
        t
      }
    }, len(0), Some(MimeType(mimeTypeCode(0), code2Types(mimeTypeCode(0)))))
    blob
  }

  def buildPut(blob: Blob, blobId: BlobId): Put = {
    val rowCol = blobId2RowCol(blobId)
    val retPut: Put = new Put(rowCol._1)
    val cellData = blobToCellData(blob)
    retPut.addColumn(columnFamily, rowCol._2, cellData)
    retPut
  }

  def buildDelete(blobId: BlobId): Delete = {
    val rowCol = blobId2RowCol(blobId)
    val delete: Delete = new Delete(rowCol._1)
    delete.addColumns(columnFamily, rowCol._2)
  }

  def buildBlobGet(blobId: BlobId): Get = {
    val rowCol = blobId2RowCol(blobId)
    val blobGet: Get = new Get(rowCol._1)
    blobGet.addColumn(columnFamily, rowCol._2)
  }

  def buildScan(): Scan = {
    new Scan()
  }

  def buildBlobFromGetResult(res: Result): List[(BlobId, Blob)] = {
    if (!res.isEmpty) {
      val row = res.getRow
      val buffer = new ArrayBuffer[(BlobId, Blob)]()

      for (entry: util.Map.Entry[Array[Byte], Array[Byte]] <- res.getFamilyMap(columnFamily).entrySet()) {
        val blobId = BlobId.fromBytes(Array.concat(row, entry.getKey))
        val blob = cellDataToBlob(entry.getValue)

        buffer.append((blobId, blob))
      }
      buffer.toList
    }
    else null
  }
}


