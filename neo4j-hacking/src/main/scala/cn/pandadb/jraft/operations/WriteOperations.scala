package cn.pandadb.jraft.operations

import java.nio.ByteBuffer

import scala.collection.mutable.ArrayBuffer
import org.neo4j.values.storable.{Value => Neo4jValue}
import org.neo4j.graphdb.GraphDatabaseService
import cn.pandadb.driver.values.{Value => PandaDBValue}
import cn.pandadb.jraft.PandaJraftServer
import cn.pandadb.util.ValueConverter


class WriteOperations extends Serializable {

  val ops: ArrayBuffer[TxOperation] = ArrayBuffer[TxOperation]();

  def doSerialize(): Unit = {
    println("=======================")
    import java.io.ObjectOutputStream
    val oos = new ObjectOutputStream(System.out)
    ops.foreach(op => {
      oos.writeObject(op)
    })
  }

  def applyTxOpeartionsToDB(db: GraphDatabaseService): Unit = {
    val tx = db.beginTx()
    println(ops)
    ops.foreach( op => {
      op match {
        case op1: NodeCreateWithId => db.createNode()
        case op1: NodeCreateWithLabels => println(op1) // db.createNode(op1.id)
        case op1: Any => println("no matched "+op1.getClass)
      }
    })
    tx.success()
    tx.close()
  }

  def nodeCreateWithId(id: Long): Unit = {
    ops.append(NodeCreateWithId(id))
  }
  def nodeCreateWithLabels (id: Long, labels: Array[Int]): Unit = {
    ops.append(NodeCreateWithLabels(id, labels))
  }
  def nodeDelete (node: Long): Unit = {
    ops.append(NodeDelete(node))
  }
  def nodeDetachDelete (nodeId: Long): Unit = {
    ops.append(NodeDetachDelete(nodeId))
  }
  def relationshipCreate(sourceNode: Long, relationshipType: Int, targetNode: Long): Unit = {
    ops.append(RelationshipCreate(sourceNode, relationshipType, targetNode))
  }
  def relationshipDelete (relationship: Long): Unit = {
    ops.append(RelationshipDelete(relationship))
  }
  def nodeAddLabel(node: Long, nodeLabel: Int): Unit = {
    ops.append(NodeAddLabel(node, nodeLabel))
  }
  def nodeRemoveLabel(node: Long, labelId: Int): Unit = {
    ops.append(NodeRemoveLabel(node, labelId))
  }
  def nodeSetProperty(node: Long, propertyKey: Int, value: Neo4jValue): Unit = {
    ops.append(NodeSetProperty(node, propertyKey, ValueConverter.convertValue(value)))
  }
  def nodeRemoveProperty(node: Long, propertyKey: Int): Unit = {
    ops.append(NodeRemoveProperty(node, propertyKey))
  }
  def relationshipSetProperty(relationship: Long, propertyKey: Int, value: Neo4jValue): Unit = {
    ops.append(RelationshipSetProperty(relationship, propertyKey, ValueConverter.convertValue(value)))
  }
  def relationshipRemoveProperty(relationship: Long, propertyKey: Int): Unit = {
    ops.append(RelationshipRemoveProperty(relationship, propertyKey))
  }
  def graphSetProperty(propertyKey: Int, value: Neo4jValue): Unit = {
    ops.append(GraphSetProperty(propertyKey, ValueConverter.convertValue(value)))
  }

}
