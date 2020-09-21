package org.neo4j.driver.internal;

import org.neo4j.driver.GraphDatabase;
import scala.util.matching.Regex;
import utils.UtilsForPanda;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.regex.Pattern;

public class PandaUtils {
    UtilsForPanda utils = new UtilsForPanda();

    public boolean isWriteCypher(String cypher) {
       return utils.isWriteStatement(cypher);
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
