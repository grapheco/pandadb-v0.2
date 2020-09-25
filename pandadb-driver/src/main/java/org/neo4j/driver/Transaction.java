//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package org.neo4j.driver;

import org.neo4j.driver.util.Resource;

public interface Transaction extends Resource, StatementRunner {
    void success();

    void failure();

    void close();

    // NOTE: pandadb
    void closeForPanda();
    //END_NOTE: pandadb

}
