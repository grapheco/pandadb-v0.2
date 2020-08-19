package cn.pandadb.jraft.rpc

class Neo4jBoltAddressValue(bolt: String) extends Serializable {

  override def toString(): String = {
    bolt
  }
}
