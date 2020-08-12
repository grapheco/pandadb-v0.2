package cn.pandadb.driver

import java.util.Locale

object utils {

  def isWriteStatement(cypherStr: String): Boolean = {
    val lowerCypher = cypherStr.toLowerCase(Locale.ROOT)
    if (lowerCypher.contains("explain")) {
      false
    } else if (lowerCypher.contains("create") || lowerCypher.contains("merge") ||
      lowerCypher.contains("set") || lowerCypher.contains("delete")) {
      true
    } else {
      false
    }
  }
}
