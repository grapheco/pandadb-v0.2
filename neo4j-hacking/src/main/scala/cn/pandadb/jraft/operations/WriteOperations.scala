package cn.pandadb.jraft.operations

import java.nio.ByteBuffer

import scala.collection.mutable.ArrayBuffer
import org.neo4j.values.storable.{Value}
import org.neo4j.graphdb.{GraphDatabaseService, Label, RelationshipType}

// scalastyle:off println
class WriteOperations extends Serializable {

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
        case op1: Any => println("no matched "+op1.getClass)
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
    ops.append(NodeSetProperty(node, propertyKey, value.asObjectCopy()))
  }
  def nodeRemoveProperty(node: Long, propertyKey: String): Unit = {
    ops.append(NodeRemoveProperty(node, propertyKey))
  }
  def relationshipSetProperty(relationship: Long, propertyKey: String, value: Value): Unit = {
    ops.append(RelationshipSetProperty(relationship, propertyKey, value.asObjectCopy()))
  }
  def relationshipRemoveProperty(relationship: Long, propertyKey: String): Unit = {
    ops.append(RelationshipRemoveProperty(relationship, propertyKey))
  }
  def graphSetProperty(propertyKey: String, value: Value): Unit = {
    ops.append(GraphSetProperty(propertyKey, value.asObjectCopy()))
  }

}
