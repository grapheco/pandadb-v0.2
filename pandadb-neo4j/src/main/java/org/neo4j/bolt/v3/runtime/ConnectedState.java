/*
 * Copyright (c) 2002-2019 "Neo4j,"
 * Neo4j Sweden AB [http://neo4j.com]
 *
 * This file is part of Neo4j.
 *
 * Neo4j is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.neo4j.bolt.v3.runtime;

import java.util.ArrayList;
import java.util.Map;

import cn.pandadb.config.PandaConfig;
import cn.pandadb.jraft.PandaJraftService;
import cn.pandadb.jraft.rpc.GetNeo4jBoltAddressRequest;
import cn.pandadb.jraft.rpc.Neo4jBoltAddressValue;
import cn.pandadb.server.PandaRuntimeContext;
import com.alipay.sofa.jraft.entity.PeerId;
import com.alipay.sofa.jraft.option.CliOptions;
import com.alipay.sofa.jraft.rpc.RpcClient;
import com.alipay.sofa.jraft.rpc.impl.cli.CliClientServiceImpl;
import org.neo4j.bolt.messaging.RequestMessage;
import org.neo4j.bolt.runtime.BoltConnectionFatality;
import org.neo4j.bolt.runtime.BoltStateMachineState;
import org.neo4j.bolt.runtime.StateMachineContext;
import org.neo4j.bolt.v3.messaging.request.HelloMessage;
import org.neo4j.values.storable.Values;
import scala.collection.Iterator;

import static org.neo4j.bolt.v1.runtime.BoltAuthenticationHelper.processAuthentication;
import static org.neo4j.util.Preconditions.checkState;

/**
 * Following the socket connection and a small handshake exchange to
 * establish protocol version, the machine begins in the CONNECTED
 * state. The <em>only</em> valid transition from here is through a
 * correctly authorised HELLO into the READY state. Any other action
 * results in disconnection.
 */
public class ConnectedState implements BoltStateMachineState
{
    private static final String CONNECTION_ID_KEY = "connection_id";

    private BoltStateMachineState readyState;

    private static final String JRAFT_PEERS = "jraft_peers";
    private static final String JRAFT_LEADER = "jraft_leader";
    private static final String USE_JRAFT = "use_jraft";

    @Override
    public BoltStateMachineState process( RequestMessage message, StateMachineContext context ) throws BoltConnectionFatality
    {
        assertInitialized();
        if ( message instanceof HelloMessage )
        {
            HelloMessage helloMessage = (HelloMessage) message;
            String userAgent = helloMessage.userAgent();
            Map<String,Object> authToken = helloMessage.authToken();

            if ( processAuthentication( userAgent, authToken, context ) )
            {
                context.connectionState().onMetadata( CONNECTION_ID_KEY, Values.stringValue( context.connectionId() ) );


                //NOTE: pandadb
                // transfer jraft port to bolt port
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
                                leaderUri = peerId.getIp() + ":" + res.toString().split(":")[1];
                            }
                            peersArray.add(peerId.getIp() + ":" + res.toString().split(":")[1]);
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
                // END_NOTE: pandadb
                return readyState;
            }
            else
            {
                return null;
            }
        }
        return null;
    }

    @Override
    public String name()
    {
        return "CONNECTED";
    }

    public void setReadyState( BoltStateMachineState readyState )
    {
        this.readyState = readyState;
    }

    private void assertInitialized()
    {
        checkState( readyState != null, "Ready state not set" );
    }
}
