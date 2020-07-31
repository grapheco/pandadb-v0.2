package cn.pandadb.jraft.operations

import cn.pandadb.config.PandaConfig
import cn.pandadb.costore.CustomPropertyNodeStore

import scala.collection.JavaConversions._
import cn.pandadb.jraft.PandaJraftService
import cn.pandadb.server.PandaRuntimeContext
import org.neo4j.internal.kernel.api.exceptions.{LabelNotFoundKernelException, PropertyKeyIdNotFoundKernelException}
import org.neo4j.values.storable.Value
import org.neo4j.internal.kernel.api.Token

class CustomNeo4jTxOperationsWriter(token: Token) {
  private val writeOperations = new WriteOperations()
  private val costoreTx = PandaRuntimeContext.contextGet[CustomPropertyNodeStore]().beginWriteTransaction()

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

  private def getRelationshipTypeName(realationType: Int): String = {
    // todo
    null
  }

  private def needJraftSaveOperations: Boolean = {
    PandaRuntimeContext.contextGet[PandaConfig]().useJraft && PandaRuntimeContext.contextGet[PandaJraftService]().isLeader()
  }

  private def needCoStoreSaveOpeartions: Boolean = {
    if (PandaRuntimeContext.contextGet[PandaConfig]().useCoStorage) {
      if (PandaRuntimeContext.contextGet[PandaConfig]().useJraft) {
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

  def commit(): Unit = {
    if (this.needJraftSaveOperations) {
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

    System.out.println("Neo4j Close tx")
  }
}
