package cn.pandadb.jraft.operations

import cn.pandadb.driver.values.{Value => PandaDBValue}

trait TxOperation extends Serializable {}

case class NodeCreateWithId(val id: Long) extends TxOperation {
}

case class NodeCreateWithLabels(val id: Long, val labels: Array[Int]) extends TxOperation{}

case class NodeDelete(val id: Long) extends TxOperation{}

case class NodeDetachDelete(val id: Long) extends TxOperation{}

case class RelationshipCreate(sourceNode: Long, relationshipType: Int, targetNode: Long) extends TxOperation{}

case class RelationshipDelete (relationship: Long) extends TxOperation{}

case class NodeAddLabel(node: Long, nodeLabel: Int) extends TxOperation{}

case class NodeRemoveLabel(node: Long, labelId: Int) extends TxOperation{}

case class NodeSetProperty (node: Long, propertyKey: Int, value: PandaDBValue) extends TxOperation{}
case class NodeRemoveProperty (node: Long, propertyKey: Int) extends TxOperation{}
case class RelationshipSetProperty (relationship: Long, propertyKey: Int, value: PandaDBValue) extends TxOperation{}
case class RelationshipRemoveProperty (relationship: Long, propertyKey: Int) extends TxOperation{}
case class GraphSetProperty (propertyKey: Int, value: PandaDBValue) extends TxOperation{}
case class GraphRemoveProperty (propertyKey: Int) extends TxOperation{}
