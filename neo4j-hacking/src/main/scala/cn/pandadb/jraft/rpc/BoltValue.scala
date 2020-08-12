package cn.pandadb.jraft.rpc

class BoltValue(bolt: String) extends Serializable {

  override def toString(): String = {
    bolt
  }
}
