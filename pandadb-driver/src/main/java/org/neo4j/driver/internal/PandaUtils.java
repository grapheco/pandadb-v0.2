package org.neo4j.driver.internal;

import java.util.List;
import java.util.Random;

public class PandaUtils {
    public boolean isWriteCypher(String cypher) {
        String text = cypher.toLowerCase();
        if (text.contains("explain")) {
            return false;
        } else return text.contains("create") || text.contains("merge") || text.contains("set")
                || text.contains("delete") || text.contains("remove");
    }

    public String getLeaderUri(String leaderIp) {
        return "bolt://" + leaderIp;
    }

    public String getReaderUri(List<Object> cluster) {
        int choose = (int) (System.currentTimeMillis() % cluster.size());
        return "bolt://" + cluster.get(choose).toString();
    }
}
