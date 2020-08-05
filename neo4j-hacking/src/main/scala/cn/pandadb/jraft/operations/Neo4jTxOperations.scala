package cn.pandadb.jraft.operations

trait TxOperation extends Serializable {}

case class NodeCreateWithId(val id: Long) extends TxOperation {
}

case class NodeCreateWithLabels(val id: Long, val labels: Array[String]) extends TxOperation{}

case class NodeDelete(val id: Long) extends TxOperation{}

case class NodeDetachDelete(val id: Long) extends TxOperation{}

case class RelationshipCreateWithId(val id: Long, sourceNode: Long, relationshipType: String, targetNode: Long) extends TxOperation{}

case class RelationshipDelete (relationship: Long) extends TxOperation{}

case class NodeAddLabel(node: Long, nodeLabel: String) extends TxOperation{}

case class NodeRemoveLabel(node: Long, label: String) extends TxOperation{}

case class NodeSetProperty (node: Long, propertyKey: String, value: Object) extends TxOperation{}
case class NodeRemoveProperty (node: Long, propertyKey: String) extends TxOperation{}
case class RelationshipSetProperty (relationship: Long, propertyKey: String, value: Object) extends TxOperation{}
case class RelationshipRemoveProperty (relationship: Long, propertyKey: String) extends TxOperation{}
case class GraphSetProperty (propertyKey: String, value: Object) extends TxOperation{}
case class GraphRemoveProperty (propertyKey: String) extends TxOperation{}
