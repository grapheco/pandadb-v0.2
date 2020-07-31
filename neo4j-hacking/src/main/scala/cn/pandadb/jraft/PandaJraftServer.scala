package cn.pandadb.jraft

import java.io.{File, IOException}

import com.alipay.sofa.jraft.{Node, RaftGroupService}
import com.alipay.sofa.jraft.conf.Configuration
import com.alipay.sofa.jraft.entity.PeerId
import com.alipay.sofa.jraft.option.NodeOptions
import com.alipay.sofa.jraft.rpc.{RaftRpcServerFactory, RpcServer}
import org.apache.commons.io.FileUtils

import org.neo4j.graphdb.GraphDatabaseService

class PandaJraftServer(neo4jDB: GraphDatabaseService,
                       dataPath: String,
                       groupId: String,
                       serverIdStr: String,
                       initConfStr: String) {

  private var raftGroupService: RaftGroupService = null
  private var node: Node = null
  private var fsm: PandaGraphStateMachine = null

  // 解析参数
  val serverId: PeerId = new PeerId()
  if (!serverId.parse(serverIdStr)) throw new IllegalArgumentException("Fail to parse serverId:" + serverIdStr)
  val initConf = new Configuration()
  if (!initConf.parse(initConfStr)) throw new IllegalArgumentException("Fail to parse initConf:" + initConfStr)

  def start(): Unit = {
    // 初始化路径
    FileUtils.forceMkdir(new File(dataPath))
    // 这里让 raft RPC 和业务 RPC 使用同一个 RPC server, 通常也可以分开
    val rpcServer: RpcServer = RaftRpcServerFactory.createRaftRpcServer(serverId.getEndpoint)
    // 注册业务处理器
    //    val counterService = new CounterServiceImpl(this)
    //    rpcServer.registerProcessor(new GetValueRequestProcessor(counterService))
    //    rpcServer.registerProcessor(new IncrementAndGetRequestProcessor(counterService))
    // 初始化状态机
    this.fsm = new PandaGraphStateMachine(neo4jDB)

    // 设置NodeOption
      val nodeOptions = new NodeOptions

    // 设置初始集群配置
    nodeOptions.setInitialConf(initConf)

    // 为了测试,调整 snapshot 间隔等参数
    // 设置选举超时时间为 1 秒
    nodeOptions.setElectionTimeoutMs(5000)
    // 关闭 CLI 服务。
    nodeOptions.setDisableCli(false)
    // 每隔30秒做一次 snapshot
    nodeOptions.setSnapshotIntervalSecs(30)

    // 设置状态机到启动参数
    nodeOptions.setFsm(this.fsm)
    // 设置存储路径
    // 日志, 必须
    nodeOptions.setLogUri(dataPath + File.separator + "log")
    // 元信息, 必须
    nodeOptions.setRaftMetaUri(dataPath + File.separator + "raft_meta")
    // snapshot, 可选, 一般都推荐
//    nodeOptions.setSnapshotUri(dataPath + File.separator + "snapshot")
    // 初始化 raft group 服务框架
    this.raftGroupService = new RaftGroupService(groupId, serverId, nodeOptions, rpcServer)

    // 启动
    this.node = this.raftGroupService.start
    System.out.println("Started counter server at port:" + this.node.getNodeId.getPeerId.getPort)
  }

  def shutdown(): Unit = {
    this.node.shutdown()
  }

  def getFsm: PandaGraphStateMachine = this.fsm

  def getNode: Node = this.node

  def isLeader: Boolean = this.getNode.isLeader

  def getRaftGroupService: RaftGroupService = this.raftGroupService

  /**
    * Redirect request to new leader
    */
//  def redirect: ValueResponse = {
//    val response = new ValueResponse
//    response.setSuccess(false)
//    if (this.node != null) {
//      val leader = this.node.getLeaderId
//      if (leader != null) response.setRedirect(leader.toString)
//    }
//    response
//  }

}


//object PandaJraftServer {
//  private var instance: PandaJraftServer = null
//
//  def apply(neo4jDB: GraphDatabaseService,
//            dataPath: String,
//            groupId: String,
//            serverIdStr: String,
//            initConfStr: String): PandaJraftServer = {
//    if (instance == null) {
//      instance = new PandaJraftServer(neo4jDB, dataPath, groupId, serverIdStr, initConfStr)
//    }
//    instance
//  }
//
//  def getInstance(): PandaJraftServer = instance
//
//}