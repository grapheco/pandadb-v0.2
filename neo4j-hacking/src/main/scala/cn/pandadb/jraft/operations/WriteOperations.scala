package cn.pandadb.jraft.operations

import java.io.InputStream
import java.nio.ByteBuffer
import java.util
import java.util.Collections.singletonList

import cn.pandadb.server.{Logging, PandaRuntimeContext}

import scala.collection.JavaConversions._
import org.neo4j.blob.{Blob, BlobEntry, BlobId, InputStreamSource, MimeType}

import scala.collection.mutable.ArrayBuffer
import org.neo4j.values.storable.{BlobArray, BlobValue, CoordinateReferenceSystem, PointArray, PointValue, Value}
import org.neo4j.graphdb.{GraphDatabaseService, Label, RelationshipType}
import org.neo4j.graphdb.spatial.{CRS, Coordinate, Point}

// scalastyle:off println
class WriteOperations extends Serializable with Logging{

  private val ops: ArrayBuffer[TxOperation] = ArrayBuffer[TxOperation]();

  def size: Int = ops.size

  def applyTxOpeartionsToDB(db: GraphDatabaseService): Unit = {
    val tx = db.beginTx()
    ops.foreach( op => {
      op match {
          // todo
        case op1: NodeCreateWithId => db.createNode(op1.id)
        case op1: NodeCreateWithLabels => val labels: Array[Label] = new Array[Label](op1.labels.size)
          for (i <- 0 to op1.labels.size-1) {
            labels(i) = Label.label(op1.labels(i))
          }
          db.createNode(op1.id, labels: _*)
        case op1: NodeDelete => db.getNodeById(op1.id).delete()
        case op1: NodeDetachDelete => db.getNodeById(op1.id).delete()
        case op1: RelationshipDelete => db.getRelationshipById(op1.relationship).delete()
        case op1: NodeAddLabel => db.getNodeById(op1.node).addLabel(Label.label(op1.nodeLabel))
        case op1: NodeRemoveLabel => db.getNodeById(op1.node).removeLabel(Label.label(op1.label))
        case op1: NodeSetProperty => db.getNodeById(op1.node).setProperty(op1.propertyKey, op1.value)
        case op1: NodeRemoveProperty => db.getNodeById(op1.node).removeProperty(op1.propertyKey)
        case op1: RelationshipCreateWithId => db.getNodeById(op1.sourceNode).
          createRelationshipTo(db.getNodeById(op1.targetNode), RelationshipType.withName(op1.relationshipType), op1.id)
        case op1: RelationshipSetProperty => db.getRelationshipById(op1.relationship).setProperty(op1.propertyKey, op1.value)
        case op1: RelationshipRemoveProperty => db.getRelationshipById(op1.relationship).removeProperty(op1.propertyKey)
        case op1: GraphSetProperty =>       //todo
        case op1: GraphRemoveProperty => //todo
        case op1: Any => logger.error(s"no matched class: ${op1.getClass}")
      }
    })
    tx.success()
    tx.close()
  }

  def nodeCreateWithId(id: Long): Unit = {
    ops.append(NodeCreateWithId(id))
  }
  def nodeCreateWithLabels (id: Long, labels: Array[String]): Unit = {
    ops.append(NodeCreateWithLabels(id, labels))
  }
  def nodeDelete (node: Long): Unit = {
    ops.append(NodeDelete(node))
  }
  def nodeDetachDelete (nodeId: Long): Unit = {
    ops.append(NodeDetachDelete(nodeId))
  }
  def relationshipCreate(id: Long, sourceNode: Long, relationshipType: String, targetNode: Long): Unit = {
    ops.append(RelationshipCreateWithId(id, sourceNode, relationshipType, targetNode))
  }
  def relationshipDelete (relationship: Long): Unit = {
    ops.append(RelationshipDelete(relationship))
  }
  def nodeAddLabel(node: Long, nodeLabel: String): Unit = {
    ops.append(NodeAddLabel(node, nodeLabel))
  }
  def nodeRemoveLabel(node: Long, label: String): Unit = {
    ops.append(NodeRemoveLabel(node, label))
  }
  def nodeSetProperty(node: Long, propertyKey: String, value: Value): Unit = {
    ops.append(NodeSetProperty(node, propertyKey, value))
  }
  def nodeRemoveProperty(node: Long, propertyKey: String): Unit = {
    for (i <- ops.size-1 to 0 by -1) {
      ops(i) match {
        case op: NodeSetProperty =>
          if (op.node.equals(node) && op.propertyKey.equals(propertyKey) && op.value.isInstanceOf[BlobValue]) {
            ops.remove(i)
          }
        case _ =>
      }
    }
    ops.append(NodeRemoveProperty(node, propertyKey))
  }
  def relationshipSetProperty(relationship: Long, propertyKey: String, value: Value): Unit = {
    ops.append(RelationshipSetProperty(relationship, propertyKey, value))
  }
  def relationshipRemoveProperty(relationship: Long, propertyKey: String): Unit = {
    for (i <- ops.size-1 to 0 by -1) {
      ops(i) match {
        case op: RelationshipSetProperty =>
          if (op.relationship.equals(relationship) && op.propertyKey.equals(propertyKey) && op.value.isInstanceOf[BlobValue]) {
            ops.remove(i)
          }
        case _ =>
      }
    }
    ops.append(RelationshipRemoveProperty(relationship, propertyKey))
  }
  def graphSetProperty(propertyKey: String, value: Value): Unit = {
    ops.append(GraphSetProperty(propertyKey, value))
  }

  def assureDataSerializableBeforeCommit(): Unit = {
    // assure ops can be serialized
    ops.foreach(op => op match {
        case op1: NodeSetProperty => op1.value = convertPropertyValue(op1.value)
        case op1: RelationshipSetProperty => op1.value = convertPropertyValue(op1.value)
        case _ => None
    })
  }

  class SerializableBlobId(value1: Long, value2: Long) extends BlobId(value1, value2) with Serializable {
  }

  class SerializableBlob(override val id: SerializableBlobId,
                         override val length: Long,
                         override val mimeType: MimeType,
                         @transient  override val streamSource: InputStreamSource
                               ) extends Blob with BlobEntry with Serializable {
    override def offerStream[T](consume: InputStream => T): T =
      throw new Exception("offerStream() is not support in SerialzableBlob type.")

  }

  object SerializableBlob {
    def fromBlobEntry(blobEntry: BlobEntry): SerializableBlob = {
      new SerializableBlob(new SerializableBlobId(blobEntry.id.values(0), blobEntry.id.values(1)),
        blobEntry.length, blobEntry.mimeType, null)
    }
  }

  private def convertPropertyValue(value: Any): Object = {
    value match {
      case v1: BlobValue =>
        val blobEntry: BlobEntry = PandaRuntimeContext.contextRemove[BlobEntry](v1.blob.hashCode().toString)
        if (blobEntry != null) {
          SerializableBlob.fromBlobEntry(blobEntry)
        }
        else {
          throw new Exception(s"PandaRuntimeContext cannot find BlobEntry of Blob ${v1.blob}")
        }
      case v1: BlobArray => v1.value().map(blob => convertPropertyValue(blob))
      case v1: PointValue => SerializablePoint.fromPointValue(v1)
      case v1: PointArray => v1.asObjectCopy().map(p => convertPropertyValue(p))
      case v1: Value => v1.asObjectCopy()
    }
  }

  class SerializablePoint(val crsName: String, coordinate: Array[Double]) extends Point with Serializable {
    override def getCoordinates(): util.List[Coordinate] = singletonList(new Coordinate(coordinate: _*))

    override def getCRS: CRS = {
      CoordinateReferenceSystem.byName(crsName)
    }
  }

  object SerializablePoint {
    def fromPointValue(pointValue: PointValue) : SerializablePoint = {
      new SerializablePoint(pointValue.getCRS.asInstanceOf[CoordinateReferenceSystem].getName, pointValue.coordinate())
    }
  }

}
