package cn.pandadb.config

import java.nio.file.Paths

import org.neo4j.graphdb.factory.GraphDatabaseSettings
import org.neo4j.kernel.configuration.Config

object SettingKeys {
  val jraftServerId = "cn.pandadb.jraft.server.id"  // string
  val jraftGroupId = "cn.pandadb.jraft.server.group.id"  // string
  val jraftPeerIds = "cn.pandadb.jraft.server.peers"  // string
  val useJraft = "cn.pandadb.jraft.use"  // boolean
  val useCoStorage = "cn.pandadb.costorage.use"  // boolean
  val costoreFactory = "costore.factory"
  val esHost = "costore.es.host"
  val esPort = "costore.es.port"
  val esSchema = "costore.es.schema"
  val esIndex = "costore.es.index"
  val esType = "costore.es.type"
  val esScrollSize = "costore.es.scroll.size"//, "1000").toInt
  val esScrollTime = "costore.es.scroll.time.minutes"//, "10").toInt
}

class PandaConfig(config: Config) {
  def jraftServerId: String = config.getRaw(SettingKeys.jraftServerId).get()
  def jraftGroupId: String = config.getRaw(SettingKeys.jraftGroupId).get()
  def jraftPeerIds: String = config.getRaw(SettingKeys.jraftPeerIds).get()
  def useJraft: Boolean = config.getRaw(SettingKeys.useJraft).orElse("false").toBoolean
  def useCoStorage: Boolean = config.getRaw(SettingKeys.useCoStorage).orElse("false").toBoolean

  def activeDatabase: String = config.get(GraphDatabaseSettings.active_database)
  def dbPath: String = config.get(GraphDatabaseSettings.data_directory).getAbsolutePath
  def jraftDataPath: String = Paths.get(dbPath, "jraft").toString

  def costoreFactory: String = config.getRaw(SettingKeys.costoreFactory).get()
  def esHost: String = config.getRaw(SettingKeys.esHost).get()
  def esPort: Int = config.getRaw(SettingKeys.esPort).get().toInt
  def esSchema: String = config.getRaw(SettingKeys.esSchema).get()
  def esIndex: String = config.getRaw(SettingKeys.esIndex).get()
  def esType: String = config.getRaw(SettingKeys.esType).get()
  def esScrollSize: Int = config.getRaw(SettingKeys.esScrollSize).get().toInt
  def esScrollTime: Int = config.getRaw(SettingKeys.esScrollTime).get().toInt


  override def toString: String = {
    s"""jraftServerId: ${this.jraftServerId}
      jraftGroupId: ${this.jraftGroupId}
      jraftPeerIds: ${this.jraftPeerIds}
      useJraft: ${this.useJraft}
      useCoStorage: ${this.useCoStorage}
      activeDatabase: ${this.activeDatabase}
      dbPath: ${this.dbPath}
      jraftDataPath: ${this.jraftDataPath}"""
  }
}
