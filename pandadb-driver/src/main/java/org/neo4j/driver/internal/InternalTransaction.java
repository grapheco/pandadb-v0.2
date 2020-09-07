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
import java.util.HashMap;
import java.util.Map;

public class InternalTransaction extends AbstractStatementRunner implements Transaction {
    private final ExplicitTransaction tx;
    // NOTE: pandadb
    /**
     * Call session.beginTransaction() will new a InternalTransaction.
     * So, should clear all the data.
     */
    public Map<String, Driver> driverMap = new HashMap<>();
    public Map<String, Transaction> txMap = new HashMap<>();
    private String globalLeaderUri;

    private static final PandaUtils utils = new PandaUtils();
    // END_NOTE: pandadb

    public InternalTransaction(ExplicitTransaction tx) {
        this.tx = tx;
    }

    @Override
    public void success() {
        // NOTE: pandadb
        if (!txMap.isEmpty()) {
            txMap.values().forEach(Transaction::success);
        }
        globalLeaderUri = "";
        // END_NOTE: pandadb
        tx.success();
    }

    @Override
    public void failure() {
        // NOTE: pandadb
        if (!txMap.isEmpty()) txMap.values().forEach(Transaction::failure);
        globalLeaderUri = "";
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
        // END_NOTE: pandadb
        Futures.blockingGet(tx.closeAsync(),
                () -> terminateConnectionOnThreadInterrupt("Thread interrupted while closing the transaction"));
    }

    @Override
    public StatementResult run(Statement statement) {
        // NOTE: pandadb
        /**
         * If use jraft, parse user's statement and dispatch to leader or reader.
         * No jraft, do as original.
         * @return the statementResult
         */
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
                // how to deal with the situation: running but suddenly change leader
                GraphDatabase.isDispatcher = false;
                String cypher = statement.text();
                if (utils.isWriteCypher(cypher)) {
                    String leaderUri = utils.getLeaderUri(InboundMessageDispatcher.getLeaderId());
                    if (!leaderUri.equals(globalLeaderUri)) {
                        Driver driver = GraphDatabase.driver(leaderUri, GraphDatabase.pandaAuthToken);
                        globalLeaderUri = leaderUri;
                        if (driverMap.containsKey(leaderUri)) {
                            driverMap.get(leaderUri).close();
                            driverMap.put(leaderUri, driver);
                        } else driverMap.put(leaderUri, driver);
                        Transaction tx = driver.session().beginTransaction();
                        txMap.put(leaderUri, tx);
                        return tx.run(statement);
                    }
                    Transaction tx = txMap.get(leaderUri);
                    return tx.run(statement);
                } else {
                    Driver driver;
                    Transaction tx;
                    String readerUri = utils.getReaderUri(InboundMessageDispatcher.getReaderIds());
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
}
