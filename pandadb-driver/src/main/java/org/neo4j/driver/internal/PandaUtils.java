package org.neo4j.driver.internal;

import org.neo4j.driver.GraphDatabase;

import java.util.ArrayList;
import java.util.Arrays;
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

    public String getReaderUri(List<Object> cluster, boolean isIncludeLeader) {
        List<Object> tempCluster = new ArrayList<>(cluster);
        if (!isIncludeLeader) {
            tempCluster.remove(GraphDatabase.getLeaderId());
        }
        int choose = (int) (System.currentTimeMillis() % tempCluster.size());
        return "bolt://" + tempCluster.get(choose).toString();
    }
}
