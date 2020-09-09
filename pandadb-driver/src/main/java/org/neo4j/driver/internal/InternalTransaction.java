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
import org.neo4j.driver.internal.util.Futures;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

public class InternalTransaction extends AbstractStatementRunner implements Transaction {
    private final ExplicitTransaction tx;
    // NOTE: pandadb
    /*
     * Call session.beginTransaction() will new an InternalTransaction.
     */
    private Transaction leaderTx = null;
    private Driver leaderDriver = null;
    public Map<String, Driver> driverMap = new HashMap<>();
    public Map<String, Transaction> txMap = new HashMap<>();
    public LinkedList<Statement> cypherLogs = new LinkedList<>();

    private static final PandaUtils utils = new PandaUtils();
    // END_NOTE: pandadb

    public InternalTransaction(ExplicitTransaction tx) {
        this.tx = tx;
    }

    @Override
    public void success() {
        // NOTE: pandadb
        if (!txMap.isEmpty()) txMap.values().forEach(Transaction::success);
        if (leaderTx != null) leaderTx.success();
        if (!cypherLogs.isEmpty()) cypherLogs.clear();
        // END_NOTE: pandadb
        tx.success();
    }

    @Override
    public void failure() {
        // NOTE: pandadb
        if (!txMap.isEmpty()) txMap.values().forEach(Transaction::failure);
        if (leaderTx != null) leaderTx.failure();
        // END_NOTE: pandadb
        tx.failure();
    }

    @Override
    public void close() {
        // NOTE: pandadb
        if (!txMap.isEmpty()) {
            txMap.values().forEach(Transaction::close);
            txMap.clear();
        }
        if (!driverMap.isEmpty()) {
            driverMap.values().forEach(Driver::close);
            driverMap.clear();
        }
        if (leaderTx != null) {
            leaderTx.close();
            leaderTx = null;
        }
        if (leaderDriver != null) {
            leaderDriver.close();
            leaderDriver = null;
        }
        // END_NOTE: pandadb
        Futures.blockingGet(tx.closeAsync(),
                () -> terminateConnectionOnThreadInterrupt("Thread interrupted while closing the transaction"));
    }

    @Override
    public StatementResult run(Statement statement) {
        // NOTE: pandadb
        /*
         * If use jraft, parse user's statement and dispatch to leader or reader.
         * No jraft, do as original.
         * @return the statementResult
         */
        boolean useJraft = GraphDatabase.isUseJraft();
        if (!useJraft) {
            StatementResultCursor cursor = Futures.blockingGet(tx.runAsync(statement, false),
                    () -> terminateConnectionOnThreadInterrupt("Thread interrupted while running query in transaction"));
            return new InternalStatementResult(tx.connection(), cursor);
        } else {
            if (!GraphDatabase.isDispatcher) {
                StatementResultCursor cursor = Futures.blockingGet(tx.runAsync(statement, false),
                        () -> terminateConnectionOnThreadInterrupt("Thread interrupted while running query in transaction"));
                StatementResult result = new InternalStatementResult(tx.connection(), cursor);
                GraphDatabase.isDispatcher = true;
                return result;
            } else {
                // pandadb logic
                GraphDatabase.isDispatcher = false;
                String cypher = statement.text();

                // send HelloMessage to update cluster's state.
                String savedLeaderUri = utils.getLeaderUri(GraphDatabase.getLeaderId());
                //TODO: node shutdown, this will be failed.
                GraphDatabase.driver(savedLeaderUri, GraphDatabase.pandaAuthToken).close();
                String refreshLeaderUri = utils.getLeaderUri(GraphDatabase.getLeaderId());

                //write cypher
                if (utils.isWriteCypher(cypher)) {
                    /*
                     * if changed leader, tx running in early leader should failed.
                     * all the statement should rerun in new leader.
                     */
                    if (!savedLeaderUri.equals(refreshLeaderUri)) {
                        Driver driver = GraphDatabase.driver(refreshLeaderUri, GraphDatabase.pandaAuthToken);
                        if (leaderDriver != null) {
                            leaderTx = driver.session().beginTransaction();
                            if (!cypherLogs.isEmpty()) rerunWriteCypherInNewLeader(cypherLogs, refreshLeaderUri);
                        } else {
                            leaderDriver = driver;
                            leaderTx = driver.session().beginTransaction();
                        }
                        if (driverMap.containsKey(savedLeaderUri)) {
                            driverMap.remove(savedLeaderUri);
                            txMap.remove(savedLeaderUri);
                        }
                        txMap.put(refreshLeaderUri, leaderTx);
                        driverMap.put(refreshLeaderUri, driver);
                    }
                    //leader not change
                    else {
                        if (leaderDriver == null) {
                            Driver driver = GraphDatabase.driver(refreshLeaderUri, GraphDatabase.pandaAuthToken);
                            leaderTx = driver.session().beginTransaction();
                            leaderDriver = driver;

                            txMap.put(refreshLeaderUri, leaderTx);
                            driverMap.put(refreshLeaderUri, driver);
                        }
                        cypherLogs.add(statement);
                    }
                    return leaderTx.run(statement);
                }
                //read cypher
                else {
                    // TODO: chosen node offline?
                    Driver driver;
                    Transaction tx;
                    String readerUri = utils.getReaderUri(GraphDatabase.getReaderIds());
                    if (driverMap.containsKey(readerUri)) {
                        tx = txMap.get(readerUri);
                    } else {
                        driver = GraphDatabase.driver(readerUri, GraphDatabase.pandaAuthToken);
                        tx = driver.session().beginTransaction();
                        driverMap.put(readerUri, driver);
                        txMap.put(readerUri, tx);
                    }
                    return tx.run(statement);
                }
            }
        }
        // END_NOTE: pandadb
    }

    @Override
    public boolean isOpen() {
        return tx.isOpen();
    }

    private void terminateConnectionOnThreadInterrupt(String reason) {
        tx.connection().terminateAndRelease(reason);
    }

    //NOTE: pandadb
    private void rerunWriteCypherInNewLeader(LinkedList<Statement> cyphers, String refreshLeaderUri) {
        Driver driver = GraphDatabase.driver(refreshLeaderUri, GraphDatabase.pandaAuthToken);
        Transaction tx = driver.session().beginTransaction();
        GraphDatabase.isDispatcher = false;
        while (cyphers.size() != 0) {
            tx.run(cyphers.removeFirst());
        }
        tx.success();
        tx.close();
        driver.close();
    }
    //END_NOTE: pandadb
}
