package org.neo4j.driver.internal;

import java.util.List;
import java.util.Random;

public class PandaUtils {
    private int count = 0;
    public boolean isWriteCypher(String cypher) {
        String text = cypher.toLowerCase();
        if (text.contains("explain")) {
            return false;
        } else if (text.contains("create") || text.contains("merge") || text.contains("set")
                || text.contains("delete") || text.contains("remove")) {
            return true;
        } else {
            return false;
        }
    }

    public String getLeaderUri(String cluster) {
        return "bolt://" + cluster;
    }

    public String getReaderUri(List<Object> cluster) {
        int choose = count % cluster.size();
        count += 1;
        return "bolt://" + cluster.get(choose).toString();
    }
}
