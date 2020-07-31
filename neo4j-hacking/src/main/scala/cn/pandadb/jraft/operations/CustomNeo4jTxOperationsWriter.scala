package cn.pandadb.jraft.operations

import cn.pandadb.config.PandaConfig

import scala.collection.JavaConversions._
import cn.pandadb.jraft.PandaJraftService
import cn.pandadb.server.PandaRuntimeContext
import org.neo4j.internal.kernel.api.exceptions.{LabelNotFoundKernelException, PropertyKeyIdNotFoundKernelException}
import org.neo4j.values.storable.Value
import org.neo4j.internal.kernel.api.Token

class CustomNeo4jTxOperationsWriter(token: Token) {
  private val writeOperations = new WriteOperations()

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
  }

  def nodeDelete(nodeId: Long): Unit = {
    if (this.needJraftSaveOperations) this.writeOperations.nodeDelete(nodeId)
  }

  def nodeCreateWithLabels(nodeId: Long, labels: Array[Int]): Unit = {
    if (this.needJraftSaveOperations) {
      val labelNames: Array[String] = new Array[String](labels.size)
      for (i <- 0 to labels.size-1) {
        labelNames(i) = getNodeLabelName(labels(i))
      }
      this.writeOperations.nodeCreateWithLabels(nodeId, labelNames)
    }

  }

  def nodeSetLabel(nodeId: Long, label: Int): Unit = {
    if (this.needJraftSaveOperations) {
      this.writeOperations.nodeAddLabel(nodeId, getNodeLabelName(label))
    }
  }

  def nodeRemoveLabel(nodeId: Long, label: Int): Unit = {
    if (this.needJraftSaveOperations) {
      this.writeOperations.nodeRemoveLabel(nodeId, getNodeLabelName(label))
    }
  }

  def nodeSetProperty(nodeId: Long, property: Int, value: Value): Unit = {
    if (this.needJraftSaveOperations) {
      this.writeOperations.nodeSetProperty(nodeId, getPropertyKeyName(property), value)
    }
  }

  def nodeRemoveProperty(nodeId: Long, property: Int): Unit = {
    if (this.needJraftSaveOperations) {
      this.writeOperations.nodeRemoveProperty(nodeId, getPropertyKeyName(property))
    }
  }

  def commit(): Unit = {
    if (this.needJraftSaveOperations) {
      PandaRuntimeContext.contextGet[PandaJraftService]().commitWriteOpeartions(this.writeOperations)
    }
  }

  def rollback(): Unit = {
  }

  def undo(): Unit = {
  }

  def close(): Unit = {
    System.out.println()
    System.out.println("Neo4j Close tx")
  }
}
