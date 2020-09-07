//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package org.neo4j.bolt.v3.runtime;

import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.TimeoutException;

import cn.pandadb.config.PandaConfig;
import cn.pandadb.jraft.PandaJraftService;
import cn.pandadb.jraft.rpc.GetNeo4jBoltAddressRequest;
import cn.pandadb.jraft.rpc.Neo4jBoltAddressValue;
import cn.pandadb.server.PandaRuntimeContext;
import com.alipay.sofa.jraft.JRaftUtils;
import com.alipay.sofa.jraft.RouteTable;
import com.alipay.sofa.jraft.conf.Configuration;
import com.alipay.sofa.jraft.entity.PeerId;
import com.alipay.sofa.jraft.option.CliOptions;
import com.alipay.sofa.jraft.rpc.RpcClient;
import com.alipay.sofa.jraft.rpc.impl.cli.CliClientServiceImpl;
import org.neo4j.bolt.messaging.RequestMessage;
import org.neo4j.bolt.runtime.BoltConnectionFatality;
import org.neo4j.bolt.runtime.BoltStateMachineState;
import org.neo4j.bolt.runtime.StateMachineContext;
import org.neo4j.bolt.v1.runtime.BoltAuthenticationHelper;
import org.neo4j.bolt.v3.messaging.request.HelloMessage;
import org.neo4j.cypher.internal.frontend.v2_3.ast.functions.Str;
import org.neo4j.util.Preconditions;
import org.neo4j.values.storable.Values;
import scala.collection.Iterator;
import scala.collection.immutable.Set;

public class ConnectedState implements BoltStateMachineState {
    private static final String CONNECTION_ID_KEY = "connection_id";
    private BoltStateMachineState readyState;

    private static final String JRAFT_PEERS = "jraft_peers";
    private static final String JRAFT_LEADER = "jraft_leader";
    private static final String USE_JRAFT = "use_jraft";

    public ConnectedState() {
    }

    public BoltStateMachineState process(RequestMessage message, StateMachineContext context) throws BoltConnectionFatality {
        this.assertInitialized();
        if (message instanceof HelloMessage) {
            HelloMessage helloMessage = (HelloMessage) message;
            String userAgent = helloMessage.userAgent();
            Map<String, Object> authToken = helloMessage.authToken();
            if (BoltAuthenticationHelper.processAuthentication(userAgent, authToken, context)) {
                context.connectionState().onMetadata("connection_id", Values.stringValue(context.connectionId()));


                PandaConfig config = PandaRuntimeContext.contextGet(PandaConfig.class.getName());

                if (config.useJraft()) {
                    ArrayList<String> peersArray = new ArrayList<>();
                    PandaJraftService service = PandaRuntimeContext.contextGet(PandaJraftService.class.getName());
                    Iterator<PeerId> iterator = service.getPeers().iterator();
                    PeerId leader = service.getLeader();
                    String leaderUri = "";
                    CliClientServiceImpl cliClientService = new CliClientServiceImpl();
                    cliClientService.init(new CliOptions());
                    while (iterator.hasNext()) {
                        PeerId peerId = iterator.next();
                        GetNeo4jBoltAddressRequest request = new GetNeo4jBoltAddressRequest();
                        RpcClient rpcClient = cliClientService.getRpcClient();
                        try {
                            Neo4jBoltAddressValue res = (Neo4jBoltAddressValue) rpcClient.invokeSync(peerId.getEndpoint(), request, 5000);
                            if (peerId.equals(leader)) {
                                leaderUri = peerId.getIp() + res.toString();
                            }
                            peersArray.add(peerId.getIp() + res.toString());
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }

                    context.connectionState().onMetadata(JRAFT_PEERS, Values.stringArray(peersArray.toArray(new String[peersArray.size()])));
                    context.connectionState().onMetadata(JRAFT_LEADER, Values.stringValue(leaderUri));
                    context.connectionState().onMetadata(USE_JRAFT, Values.booleanValue(true));
                } else {
                    context.connectionState().onMetadata(USE_JRAFT, Values.booleanValue(false));
                }
                return this.readyState;
            } else {
                return null;
            }
        } else {
            return null;
        }
    }

    public String name() {
        return "CONNECTED";
    }

    public void setReadyState(BoltStateMachineState readyState) {
        this.readyState = readyState;
    }

    private void assertInitialized() {
        Preconditions.checkState(this.readyState != null, "Ready state not set");
    }

    private String getJraftClusters() {
        CliClientServiceImpl cliClientService = new CliClientServiceImpl();
        cliClientService.init(new CliOptions());
        Configuration conf = JRaftUtils.getConfiguration("127.0.0.1:8081");
        RouteTable.getInstance().updateConfiguration("panda", conf);
        try {
            RouteTable.getInstance().refreshConfiguration(cliClientService, "panda", 10000);
            RouteTable.getInstance().refreshLeader(cliClientService, "panda", 10000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (TimeoutException e) {
            e.printStackTrace();
        }
        PeerId leader = RouteTable.getInstance().selectLeader("panda");
        if (leader == null) {
            return "no_leader";
        }
        return leader.toString();
    }
}
