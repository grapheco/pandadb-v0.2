package cn.pandadb.jraft.operations

import java.nio.ByteBuffer

import scala.collection.mutable.ArrayBuffer
import org.neo4j.values.storable.{Value => Neo4jValue}
import org.neo4j.graphdb.GraphDatabaseService
import org.neo4j.graphdb.{Label => Neo4jLabel, Node => Neo4jNode, Relationship => Neo4jRelationship, RelationshipType => Neo4jType}

import cn.pandadb.driver.values.{Value => PandaDBValue}
import cn.pandadb.jraft.PandaJraftServer
import cn.pandadb.util.ValueConverter


class WriteOperations extends Serializable {

  val ops: ArrayBuffer[TxOperation] = ArrayBuffer[TxOperation]();

  def applyTxOpeartionsToDB(db: GraphDatabaseService): Unit = {
    val tx = db.beginTx()
    ops.foreach( op => {
      op match {
          // todo
        case op1: NodeCreateWithId => db.createNode(op1.id)
        case op1: NodeCreateWithLabels => {
          val labels: Array[Neo4jLabel] = new Array[Neo4jLabel](op1.labels.size)
          for (i <- 0 to op1.labels.size-1) {
            labels(i) = Neo4jLabel.label(op1.labels(i))
          }
          db.createNode(op1.id, labels: _*)
        }
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
  def relationshipCreate(sourceNode: Long, relationshipType: String, targetNode: Long): Unit = {
    ops.append(RelationshipCreate(sourceNode, relationshipType, targetNode))
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
  def nodeSetProperty(node: Long, propertyKey: String, value: Neo4jValue): Unit = {
    ops.append(NodeSetProperty(node, propertyKey, ValueConverter.convertValue(value)))
  }
  def nodeRemoveProperty(node: Long, propertyKey: String): Unit = {
    ops.append(NodeRemoveProperty(node, propertyKey))
  }
  def relationshipSetProperty(relationship: Long, propertyKey: String, value: Neo4jValue): Unit = {
    ops.append(RelationshipSetProperty(relationship, propertyKey, ValueConverter.convertValue(value)))
  }
  def relationshipRemoveProperty(relationship: Long, propertyKey: String): Unit = {
    ops.append(RelationshipRemoveProperty(relationship, propertyKey))
  }
  def graphSetProperty(propertyKey: String, value: Neo4jValue): Unit = {
    ops.append(GraphSetProperty(propertyKey, ValueConverter.convertValue(value)))
  }

}
