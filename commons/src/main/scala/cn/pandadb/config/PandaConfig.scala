package cn.pandadb.config

import java.nio.file.{Path, Paths}

import org.neo4j.graphdb.factory.GraphDatabaseSettings
import org.neo4j.kernel.configuration.Config

object SettingKeys {
  val jraftServerId = "cn.pandadb.jraft.server.id"  // string
  val jraftGroupId = "cn.pandadb.jraft.server.group.id"  // string
  val jraftPeerIds = "cn.pandadb.jraft.server.peers"  // string
  val useJraft = "cn.pandadb.jraft.enabled"  // boolean
  val useCoStorage = "costore.enable"  // boolean
  val costoreFactory = "costore.factory"
  val esHost = "costore.es.host"
  val esPort = "costore.es.port"
  val esSchema = "costore.es.schema"
  val esIndex = "costore.es.index"
  val esType = "costore.es.type"
  val esScrollSize = "costore.es.scroll.size"//, "1000").toInt
  val esScrollTime = "costore.es.scroll.time.minutes" //, "10").toInt
  val bolt = "dbms.connector.bolt.listen_address"
  val useSnapshot = "cn.pandadb.jraft.server.snapshot.enable"
  val snapshotIntervalSecs = "cn.pandadb.jraft.server.snapshot.interval.seconds"
}

class PandaConfig(config: Config) {
  def useSnapshot: Boolean = config.getRaw(SettingKeys.useSnapshot).orElse("false").toBoolean
  def snapshotIntervalSecs: Int = config.getRaw(SettingKeys.snapshotIntervalSecs).orElse("60").toInt
  def bolt: String = config.getRaw(SettingKeys.bolt).get()
  def jraftServerId: String = config.getRaw(SettingKeys.jraftServerId).get()
  def jraftGroupId: String = config.getRaw(SettingKeys.jraftGroupId).get()
  def jraftPeerIds: String = config.getRaw(SettingKeys.jraftPeerIds).get()
  def useJraft: Boolean = config.getRaw(SettingKeys.useJraft).orElse("false").toBoolean
  def useCoStorage: Boolean = config.getRaw(SettingKeys.useCoStorage).orElse("false").toBoolean

  def activeDatabase: String = config.get(GraphDatabaseSettings.active_database)
  def dataPath: String = config.get(GraphDatabaseSettings.data_directory).getAbsolutePath
  def jraftDataPath: String = Paths.get(dataPath, "jraft").toString

  def costoreFactory: String = config.getRaw(SettingKeys.costoreFactory).get()
  def esHost: String = config.getRaw(SettingKeys.esHost).get()
  def esPort: Int = config.getRaw(SettingKeys.esPort).get().toInt
  def esSchema: String = config.getRaw(SettingKeys.esSchema).get()
  def esIndex: String = config.getRaw(SettingKeys.esIndex).get()
  def esType: String = config.getRaw(SettingKeys.esType).get()
  def esScrollSize: Int = config.getRaw(SettingKeys.esScrollSize).orElse("1000").toInt
  def esScrollTime: Int = config.getRaw(SettingKeys.esScrollTime).orElse("10").toInt

  def getGraphDatabasePath: Path = Paths.get(dataPath, activeDatabase).toAbsolutePath

  override def toString: String = {
    s"""jraftServerId: ${this.jraftServerId}
      jraftGroupId: ${this.jraftGroupId}
      jraftPeerIds: ${this.jraftPeerIds}
      useJraft: ${this.useJraft}
      useCoStorage: ${this.useCoStorage}
      activeDatabase: ${this.activeDatabase}
      dataPath: ${this.dataPath}
      jraftDataPath: ${this.jraftDataPath}"""
  }
}
