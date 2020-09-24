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

import org.apache.commons.lang3.exception.ExceptionContext;
import org.neo4j.driver.*;
import org.neo4j.driver.async.StatementResultCursor;
import org.neo4j.driver.internal.async.ExplicitTransaction;
import org.neo4j.driver.internal.util.Futures;
import scala.Int;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

public class InternalTransaction extends AbstractStatementRunner implements Transaction {
    private final ExplicitTransaction tx;
    // NOTE: pandadb
    private boolean hasWriteStatement = false;
    private Transaction leaderTx = null;
    private Driver leaderDriver = null;
    private Transaction readerTx = null;
    private Driver readerDriver = null;

    private static final PandaUtils utils = new PandaUtils();
    // END_NOTE: pandadb

    public InternalTransaction(ExplicitTransaction tx) {
        this.tx = tx;
    }

    @Override
    public void success() {
        // NOTE: pandadb
        if (leaderTx != null) leaderTx.success();
        if (readerTx != null) readerTx.success();
        // END_NOTE: pandadb
        tx.success();
    }

    @Override
    public void failure() {
        // NOTE: pandadb
        if (leaderTx != null) leaderTx.failure();
        if (readerTx != null) readerTx.failure();
        // END_NOTE: pandadb
        tx.failure();
    }

    @Override
    public void close() {
        // NOTE: pandadb
        try {
            if (leaderDriver != null) {
                leaderTx.close();
                leaderDriver.close();
                leaderTx = null;
                leaderDriver = null;
            }
            if (readerDriver != null) {
                readerTx.close();
                readerDriver.close();
                readerTx = null;
                readerDriver = null;
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
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
                String cypher = statement.text().toLowerCase();

                //write cypher
                if (utils.isWriteCypher(cypher)) {
                    hasWriteStatement = true;
                    if (InternalSession.leaderDriver == null) {
                        if (leaderDriver == null) {
                            Driver driver = GraphDatabase.driver(utils.getLeaderUri(GraphDatabase.getLeaderId())
                                    , GraphDatabase.pandaAuthToken);
                            leaderTx = driver.session().beginTransaction();
                            leaderDriver = driver;
                        }
                        return leaderTx.run(statement);
                    } else {
                        if (leaderTx == null) {
                            leaderTx = InternalSession.leaderSession.beginTransaction();
                            leaderDriver = InternalSession.leaderDriver;
                        }
                        return leaderTx.run(statement);
                    }
                }
                //read cypher
                else {
                    if (hasWriteStatement) {
                        return leaderTx.run(statement);
                    }
                    String readerUri = utils.getReaderUri(GraphDatabase.getReaderIds(), false);
                    if (readerDriver == null) {
                        readerDriver = GraphDatabase.driver(readerUri, GraphDatabase.pandaAuthToken);
                        readerTx = readerDriver.session().beginTransaction();
                    }
                    return readerTx.run(statement);
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
