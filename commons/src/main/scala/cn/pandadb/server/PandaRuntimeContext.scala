package cn.pandadb.server

import java.util.concurrent.atomic.AtomicBoolean

import scala.collection.mutable.{Map => MMap}

object PandaRuntimeContext {
  private val _map = MMap[String, Any]();

  @volatile
  private var _snapshotLoaded: Boolean = false

  def contextPut[T](key: String, value: T): T = {
    _map(key) = value
    value
  };

  def contextPut[T](value: T)(implicit manifest: Manifest[T]): T = contextPut[T](manifest.runtimeClass.getName, value)

  def contextGet[T](key: String): T = {
    _map(key).asInstanceOf[T]
  };

  def contextGetOption[T](key: String): Option[T] = _map.get(key).map(_.asInstanceOf[T]);

  def contextGet[T]()(implicit manifest: Manifest[T]): T = contextGet(manifest.runtimeClass.getName);

  def contextGetOption[T]()(implicit manifest: Manifest[T]): Option[T] = contextGetOption(manifest.runtimeClass.getName);

  def contextRemove[T](key: String): T = _map.remove(key).getOrElse(null).asInstanceOf[T]

  def clear(): Unit = {
    _map.clear()
  }

  def setSnapshotLoaded(loaded: Boolean): Unit = {
    _snapshotLoaded = loaded
  }

  def getSnaphotLoaded(): Boolean = _snapshotLoaded
}
