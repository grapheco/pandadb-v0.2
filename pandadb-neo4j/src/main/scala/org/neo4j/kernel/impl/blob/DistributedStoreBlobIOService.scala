/*
 * Copyright (c) 2002-2019 "Neo4j,"
 * Neo4j Sweden AB [http://neo4j.com]
 *
 * This file is part of Neo4j.
 *
 * Neo4j is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.neo4j.kernel.impl.blob

import java.io.InputStream
import java.nio.ByteBuffer

import cn.pandadb.config.PandaConfig
import cn.pandadb.jraft.PandaJraftService
import cn.pandadb.server.PandaRuntimeContext
import org.apache.commons.lang.NotImplementedException
import org.neo4j.blob._
import org.neo4j.blob.impl.BlobFactory
import org.neo4j.blob.util._
import org.neo4j.kernel.impl.store.record.{PrimitiveRecord, PropertyBlock, PropertyRecord}
import org.neo4j.values.storable.{BlobArray, BlobArrayProvider, BlobValue}

/**
  * Created by lzx on 2020/8/28.
  */
class DistributedStoreBlobIOService extends StoreBlobIOService {
  override def saveBlobArray(buf: ByteBuffer, blobs: Array[Blob], ic: ContextMap): Unit = {
    if (blobs.length > 0) {
      if (blobs(0).isInstanceOf[BlobEntry]) { // used for follower nodes save blob array
        val gid: BlobId = blobs(0).asInstanceOf[BlobEntry].id
        if (ic.get[BlobStorage].existsGroup(gid)) {
          buf.put(gid.asByteArray())
        } else {
          throw new Exception(s"the BlobArray is not in BlobStorage: BlobGroupId(${gid}) BlobStorage(${ic.get[BlobStorage].getClass})")
        }
      }
      else {  // used for leader node save blob array
        val gid = ic.get[BlobStorage].saveGroup(blobs);
        if (PandaRuntimeContext.contextGet[PandaConfig]().useJraft && PandaRuntimeContext.contextGet[PandaJraftService]().isLeader()) {
          blobs.foreach(blob => {
            val blobEntry = BlobFactory.makeEntry(gid, blob)
            PandaRuntimeContext.contextPut(blob.hashCode().toString, blobEntry)
          })
        }
        val bytes = gid.asByteArray()
        buf.put(bytes)
      }
    }
  }

  override def saveBlob(ic: ContextMap, blob: Blob, keyId: Int, block: PropertyBlock): Unit = {
    if (blob.isInstanceOf[BlobEntry]) { // used for follower nodes save blob
      val blobEntry = blob.asInstanceOf[BlobEntry]
      if (ic.get[BlobStorage].exists(blobEntry.id)) {
        block.setValueBlocks(BlobIO._pack(blobEntry, keyId));
      } else {
        throw new Exception(s"the blob is not in BlobStorage: BlobId(${blobEntry.id}) BlobStorage(${ic.get[BlobStorage].getClass})")
      }
    }
    else {  // used for leader node save blob
      val bid = ic.get[BlobStorage].save(blob);
      val blobEntry = BlobFactory.makeEntry(bid, blob)
      if (PandaRuntimeContext.contextGet[PandaConfig]().useJraft && PandaRuntimeContext.contextGet[PandaJraftService]().isLeader()) {
        PandaRuntimeContext.contextPut(blob.hashCode().toString, blobEntry)
      }
      block.setValueBlocks(BlobIO._pack(blobEntry, keyId));
    }
  }

  override def deleteBlobArrayProperty(ic: ContextMap, blobs: BlobArray): Unit = {
    val gid = blobs.groupId()
    if (gid != null) {
      ic.get[BlobStorage].deleteGroup(gid)
    }
  }

  override def deleteBlobProperty(ic: ContextMap, block: PropertyBlock): Unit = {
    val entry = BlobIO.unpack(block.getValueBlocks);
    if (ic.get[BlobStorage].exists(entry.id)) { // if blobstorage is shared by multi-nodes like hbase
      ic.get[BlobStorage].delete(entry.id);
    }
  }

  override def readBlobArray(ic: ContextMap, dataBuffer: ByteBuffer, arrayLength: Int): BlobArray = {
    val bytes = BlobId.EMPTY.asByteArray()
    dataBuffer.get(bytes)
    val bid = BlobId.fromBytes(bytes)
    new BlobArray(bid, new BlobArrayProvider() {
      override def get(): Array[Blob] = ic.get[BlobStorage]().loadGroup(bid).get
    })
  }

  override def readBlobValue(ic: ContextMap, values: Array[Long]): BlobValue = {
    val entry = BlobIO.unpack(values);
    val storage = ic.get[BlobStorage];

    val blob = BlobFactory.makeStoredBlob(entry, new InputStreamSource {
      override def offerStream[T](consume: (InputStream) => T): T = {
        val bid = entry.id;
        storage.load(bid).getOrElse(throw new BlobNotExistException(bid)).offerStream(consume)
      }
    });

    BlobValue(blob);
  }
}