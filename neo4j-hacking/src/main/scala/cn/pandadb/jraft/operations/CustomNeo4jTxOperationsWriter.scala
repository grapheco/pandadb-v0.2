package cn.pandadb.jraft.operations

import cn.pandadb.config.PandaConfig
import cn.pandadb.costore.{CustomPropertyNodeStore, PropertyWriteTransaction}

import scala.collection.JavaConversions._
import cn.pandadb.jraft.PandaJraftService
import cn.pandadb.server.PandaRuntimeContext
import org.neo4j.internal.kernel.api.exceptions.{KernelException, LabelNotFoundKernelException, PropertyKeyIdNotFoundKernelException}
import org.neo4j.values.storable.Value
import org.neo4j.internal.kernel.api.Token

class CustomNeo4jTxOperationsWriter(token: Token) {
  private val config: PandaConfig = PandaRuntimeContext.contextGet[PandaConfig]()
  private var writeOperations: WriteOperations = null
  private var costoreTx: PropertyWriteTransaction = null

  private def getNodeLabelName(label: Int): String = {
    try
      token.nodeLabelName(label)
    catch {
      case e: LabelNotFoundKernelException =>
        throw new IllegalStateException("Label retrieved through kernel API should exist.", e)
    }
  }

  private def getPropertyKeyName(property: Int): String = {
    try
      token.propertyKeyName(property)
    catch {
      case e: PropertyKeyIdNotFoundKernelException =>
        throw new IllegalStateException("Property key retrieved through kernel API should exist.", e)
    }
  }

  private def getRelationshipTypeName(relationType: Int): String = {
    try
      token.relationshipTypeName(relationType)
    catch {
      case e: KernelException =>
        throw new IllegalStateException("RelationType retrieved through kernel API should exist.", e)
    }
  }

  private def needJraftSaveOperations: Boolean = {
    config.useJraft && PandaRuntimeContext.contextGet[PandaJraftService]().isLeader()
  }

  private def needCoStoreSaveOpeartions: Boolean = {
    if (config.useCoStorage) {
      if (config.useJraft) {
        PandaRuntimeContext.contextGet[PandaJraftService]().isLeader()
      }
      else {
        true
      }
    }
    else {
      false
    }
  }

  def initialize(): Unit = {
    if (needJraftSaveOperations) {
      writeOperations = new WriteOperations()
    }
    if (needCoStoreSaveOpeartions) {
      costoreTx = PandaRuntimeContext.contextGet[CustomPropertyNodeStore]().beginWriteTransaction()
    }
  }

  def nodeCreate(nodeId: Long): Unit = {
    if (this.needJraftSaveOperations) this.writeOperations.nodeCreateWithId(nodeId)
    if (this.needCoStoreSaveOpeartions) this.costoreTx.addNode(nodeId)
  }

  def nodeDelete(nodeId: Long): Unit = {
    if (this.needJraftSaveOperations) this.writeOperations.nodeDelete(nodeId)
    if (this.needCoStoreSaveOpeartions) this.costoreTx.deleteNode(nodeId)

  }

  def nodeCreateWithLabels(nodeId: Long, labels: Array[Int]): Unit = {
    if (this.needJraftSaveOperations || this.needCoStoreSaveOpeartions) {
      val labelNames: Array[String] = new Array[String](labels.size)

      if (this.needCoStoreSaveOpeartions) this.costoreTx.addNode(nodeId)

      for (i <- 0 to labels.size-1) {
        val labelName = getNodeLabelName(labels(i))
        labelNames(i) = labelName
        if (this.needCoStoreSaveOpeartions) this.costoreTx.addLabel(nodeId, labelName)
      }
      if (this.needJraftSaveOperations) {
        this.writeOperations.nodeCreateWithLabels(nodeId, labelNames)
      }
    }
  }

  def nodeSetLabel(nodeId: Long, label: Int): Unit = {
    if (this.needJraftSaveOperations) {
      this.writeOperations.nodeAddLabel(nodeId, getNodeLabelName(label))
    }
    if (this.needCoStoreSaveOpeartions) {
      this.costoreTx.addLabel(nodeId, getNodeLabelName(label))
    }
  }

  def nodeRemoveLabel(nodeId: Long, label: Int): Unit = {
    if (this.needJraftSaveOperations) {
      this.writeOperations.nodeRemoveLabel(nodeId, getNodeLabelName(label))
    }
    if (this.needCoStoreSaveOpeartions) {
      this.costoreTx.removeLabel(nodeId, getNodeLabelName(label))
    }
  }

  def nodeSetProperty(nodeId: Long, property: Int, value: Value): Unit = {
    if (this.needJraftSaveOperations) {
      this.writeOperations.nodeSetProperty(nodeId, getPropertyKeyName(property), value)
    }
    if (this.needCoStoreSaveOpeartions) {
      this.costoreTx.addProperty(nodeId, getPropertyKeyName(property), value)
    }
  }

  def nodeRemoveProperty(nodeId: Long, property: Int): Unit = {
    if (this.needJraftSaveOperations) {
      this.writeOperations.nodeRemoveProperty(nodeId, getPropertyKeyName(property))
    }
    if (this.needCoStoreSaveOpeartions) {
      this.costoreTx.removeProperty(nodeId, getPropertyKeyName(property))
    }
  }

  def relationshipCreate(relId: Long, sourceNode: Long, relationshipType: Int, targetNode: Long): Unit = {
    if (this.needJraftSaveOperations) {
      this.writeOperations.relationshipCreate(relId, sourceNode, getRelationshipTypeName(relationshipType), targetNode)
    }
  }
  def relationshipDelete (relationship: Long): Unit = {
    if (this.needJraftSaveOperations) {
      this.writeOperations.relationshipDelete(relationship)
    }
  }
  def relationshipSetProperty(relationship: Long, propertyKey: Int, value: Value): Unit = {
    if (this.needJraftSaveOperations) {
      this.writeOperations.relationshipSetProperty(relationship, getPropertyKeyName(propertyKey), value)
    }
  }
  def relationshipRemoveProperty(relationship: Long, propertyKey: Int): Unit = {
    if (this.needJraftSaveOperations) {
      this.writeOperations.relationshipRemoveProperty(relationship, getPropertyKeyName(propertyKey))
    }
  }

  def commit(): Unit = {
    println("neo4j tx commit")
    if (this.needJraftSaveOperations && this.writeOperations != null && this.writeOperations.size>0) {
      this.writeOperations.assureDataSerializableBeforeCommit()
      PandaRuntimeContext.contextGet[PandaJraftService]().commitWriteOpeartions(this.writeOperations)
    }
    if (this.needCoStoreSaveOpeartions) {
      this.costoreTx.commit()
    }
  }

  def rollback(): Unit = {
    if (this.needCoStoreSaveOpeartions) {
      this.costoreTx.rollback()
    }
  }

  def undo(): Unit = {
  }

  def close(): Unit = {
    if (this.needCoStoreSaveOpeartions) {
      this.costoreTx.close()
    }

    println("Neo4j Close tx")
  }
}
