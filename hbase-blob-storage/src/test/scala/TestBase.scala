import java.io.{File, FileInputStream}

import cn.pandadb.database.PandaDB
import org.apache.commons.io.{FileUtils, IOUtils}
import org.junit.Before
import org.neo4j.blob.impl.BlobFactory
import org.neo4j.graphdb.GraphDatabaseService

class TestBase {
  @Before
  def setup(): Unit = {
    setupNewDatabase()
  }

  private def setupNewDatabase(): Unit = {
    FileUtils.deleteDirectory(new File("./testoutput/testdb"));
    //create a new database
    if (true) {
      val db = openDatabase();
      val tx = db.beginTx();
      //create a node
      val node1 = db.createNode();

      node1.setProperty("name", "bob");
      node1.setProperty("age", 30);
      //property as a byte array
      node1.setProperty("bytes", IOUtils.toByteArray(new FileInputStream(new File("./testinput/ai/test.png"))));

      //with a blob property
      node1.setProperty("photo", BlobFactory.fromFile(new File("./testinput/ai/test.png")));
      //blob array
      node1.setProperty("photo2", (0 to 1).map(x => BlobFactory.fromFile(new File("./testinput/ai/test.png"))).toArray);

      val node2 = db.createNode();

      node2.setProperty("name", "alex");
      //with a blob property
      node2.setProperty("photo", BlobFactory.fromFile(new File("./testinput/ai/test1.png")));

      tx.success();
      tx.close();
      db.shutdown();
    }
  }

  def openDatabase(): GraphDatabaseService =
    PandaDB.openDatabase(new File("./testoutput/testdb/data/databases/graph.db"),
      new File("./testinput/neo4j.conf"));
}
