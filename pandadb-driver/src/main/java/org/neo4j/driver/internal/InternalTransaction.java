/*
 * Copyright (c) 2002-2019 "Neo4j,"
 * Neo4j Sweden AB [http://neo4j.com]
 *
 * This file is part of Neo4j.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.neo4j.driver.internal;

import org.neo4j.driver.*;
import org.neo4j.driver.async.StatementResultCursor;
import org.neo4j.driver.internal.async.ExplicitTransaction;
import org.neo4j.driver.internal.async.inbound.InboundMessageDispatcher;
import org.neo4j.driver.internal.util.Futures;

import java.util.ArrayList;

public class InternalTransaction extends AbstractStatementRunner implements Transaction {
    private final ExplicitTransaction tx;
    private ArrayList<Transaction> driverTxs = new ArrayList<>();
    private ArrayList<Session> driverSessions = new ArrayList<>();
    private ArrayList<Driver> pandaDrivers = new ArrayList<>();
    private String globalLeaderUri;
    private Driver leaderDriver;
    private Session leaderSession;
    private Transaction leaderTx;

    public InternalTransaction(ExplicitTransaction tx) {
        this.tx = tx;
    }

    @Override
    public void success() {
        driverTxs.forEach(Transaction::success);
        driverSessions.forEach(Session::close);
        pandaDrivers.forEach(Driver::close);
        driverTxs.clear();
        driverSessions.clear();
        pandaDrivers.clear();
        globalLeaderUri = "";
        tx.success();
    }

    @Override
    public void failure() {
        driverTxs.forEach(Transaction::failure);
        driverSessions.forEach(Session::close);
        pandaDrivers.forEach(Driver::close);
        driverTxs.clear();
        driverSessions.clear();
        pandaDrivers.clear();
        globalLeaderUri = "";
        tx.failure();
    }

    @Override
    public void close() {
        Futures.blockingGet(tx.closeAsync(),
                () -> terminateConnectionOnThreadInterrupt("Thread interrupted while closing the transaction"));
    }

    @Override
    public StatementResult run(Statement statement) {
        boolean useJraft = InboundMessageDispatcher.isUseJraft();
        if (!useJraft) {
            StatementResultCursor cursor = Futures.blockingGet(tx.runAsync(statement, false),
                    () -> terminateConnectionOnThreadInterrupt("Thread interrupted while running query in transaction"));
            StatementResult result = new InternalStatementResult(tx.connection(), cursor);
            GraphDatabase.isDispatcher = true;
            return result;
        } else {
            if (!GraphDatabase.isDispatcher) {
                StatementResultCursor cursor = Futures.blockingGet(tx.runAsync(statement, false),
                        () -> terminateConnectionOnThreadInterrupt("Thread interrupted while running query in transaction"));
                StatementResult result = new InternalStatementResult(tx.connection(), cursor);
                GraphDatabase.isDispatcher = true;
                return result;
            } else {
                GraphDatabase.isDispatcher = false;
                PandaUtils utils = new PandaUtils();
                String cypher = statement.text();
                if (utils.isWriteCypher(cypher)) {
                    String leaderUri = utils.getLeaderUri(InboundMessageDispatcher.getLeaderId());
                    if (!leaderUri.equals(globalLeaderUri)) {
                        globalLeaderUri = leaderUri;
                        leaderDriver = GraphDatabase.driver(leaderUri, GraphDatabase.pandaAuthToken);
                        leaderSession = leaderDriver.session();
                        leaderTx = leaderSession.beginTransaction();
                        pandaDrivers.add(leaderDriver);
                        driverSessions.add(leaderSession);
                        driverTxs.add(leaderTx);
                    }
                    return leaderTx.run(statement);
                } else {
                    String readerUri = utils.getReaderUri(InboundMessageDispatcher.getReaderIds());
                    Driver pandaDriver = GraphDatabase.driver(readerUri, GraphDatabase.pandaAuthToken);
                    Session driverSession = pandaDriver.session();
                    Transaction driverTx = driverSession.beginTransaction();
                    pandaDrivers.add(pandaDriver);
                    driverSessions.add(driverSession);
                    driverTxs.add(driverTx);
                    return driverTx.run(statement);
                }
            }
        }
    }

    @Override
    public boolean isOpen() {
        return tx.isOpen();
    }

    private void terminateConnectionOnThreadInterrupt(String reason) {
        tx.connection().terminateAndRelease(reason);
    }
}
