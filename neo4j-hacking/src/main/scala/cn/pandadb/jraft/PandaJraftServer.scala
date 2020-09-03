package cn.pandadb.jraft

import java.io.{File, IOException}
import scala.collection.JavaConverters._

import cn.pandadb.config.PandaConfig
import cn.pandadb.jraft.rpc.GetNeo4jBoltAddressRequestProcessor
import cn.pandadb.server.{Logging, PandaRuntimeContext}
import com.alipay.sofa.jraft.{Node, RaftGroupService}
import com.alipay.sofa.jraft.conf.Configuration
import com.alipay.sofa.jraft.entity.PeerId
import com.alipay.sofa.jraft.option.NodeOptions
import com.alipay.sofa.jraft.rpc.{RaftRpcServerFactory, RpcServer}
import com.alipay.sofa.jraft.{JRaftUtils, RouteTable}
import org.apache.commons.io.FileUtils
import org.neo4j.graphdb.GraphDatabaseService

class PandaJraftServer(neo4jDB: GraphDatabaseService,
                       dataPath: String,
                       groupId: String,
                       serverIdStr: String,
                       initConfStr: String) extends Logging{

  private var raftGroupService: RaftGroupService = null
  private var node: Node = null
  private var fsm: PandaGraphStateMachine = null

  // parse args
  val serverId: PeerId = new PeerId()
  if (!serverId.parse(serverIdStr)) throw new IllegalArgumentException("Fail to parse serverId:" + serverIdStr)
  val initConf = new Configuration()
  if (!initConf.parse(initConfStr)) throw new IllegalArgumentException("Fail to parse initConf:" + initConfStr)

  def start(): Unit = {
    // init file directory
    FileUtils.forceMkdir(new File(dataPath))

    // add business RPC service
    // (Here, the raft RPC and the business RPC use the same RPC server)
    val rpcServer: RpcServer = RaftRpcServerFactory.createRaftRpcServer(serverId.getEndpoint)
    // add business RPC processor
    rpcServer.registerProcessor(new GetNeo4jBoltAddressRequestProcessor(this))
    // init state machine
    this.fsm = new PandaGraphStateMachine(neo4jDB)

    // set NodeOption
    val nodeOptions = new NodeOptions
    // init configuration
    nodeOptions.setInitialConf(initConf)
    // set leader election timeout
    nodeOptions.setElectionTimeoutMs(5000)
    // dialbel CLI
    nodeOptions.setDisableCli(false)
    // set snapshot save period
    nodeOptions.setSnapshotIntervalSecs(getSnapshotTime())
    // set state machine args
    nodeOptions.setFsm(this.fsm)
    // set log save path (required)
    nodeOptions.setLogUri(dataPath + File.separator + "log")
    // set meta save path (required)
    nodeOptions.setRaftMetaUri(dataPath + File.separator + "raft_meta")
    // set snapshot save path (Optional)
    if (useSnapshot()) nodeOptions.setSnapshotUri(dataPath + File.separator + "snapshot")
    // init raft group
    this.raftGroupService = new RaftGroupService(groupId, serverId, nodeOptions, rpcServer)

    this.node = this.raftGroupService.start
    logger.info("Started PandaJraftServer at port:" + this.node.getNodeId.getPeerId.getPort)
  }

  def shutdown(): Unit = {
    this.node.shutdown()
  }

  def getFsm: PandaGraphStateMachine = this.fsm

  //def getPandaJraftService: PandaJraftService = this

  def getNode: Node = this.node

  def isLeader: Boolean = this.getNode.isLeader

  def getRaftGroupService: RaftGroupService = this.raftGroupService

  def getNeo4jBoltServerAddress(): String = {
    val pandaConfig: PandaConfig = PandaRuntimeContext.contextGet[PandaConfig]()
    pandaConfig.bolt
  }

  def useSnapshot(): Boolean = {
    val pandaConfig: PandaConfig = PandaRuntimeContext.contextGet[PandaConfig]()
    pandaConfig.useSnapshot
  }

  def getSnapshotTime(): Int = {
    val pandaConfig: PandaConfig = PandaRuntimeContext.contextGet[PandaConfig]()
    pandaConfig.snapshotTime
  }

  def getPeers(): Set[PeerId] = {
    RouteTable.getInstance().getConfiguration(this.groupId).getPeerSet.asScala.toSet
  }

}
