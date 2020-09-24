package org.neo4j.cypher.internal

import cn.pandadb.config.PandaConfig
import cn.pandadb.jraft.PandaJraftService
import cn.pandadb.server.PandaRuntimeContext

import scala.util.matching.Regex

class PandaUtils {
  val r1 = "(explain\\s)?match\\s*\\(.*\\s*\\{?.*\\}?\\s*\\)\\s*(where)?\\s*.*\\s*(set|remove|delete|merge)\\s*"
  val r3 = "(explain\\s+)?merge\\s*\\(.*\\s*\\{?.*\\}?\\s*\\)\\s*(where)?\\s*.*\\s*(set|remove|delete|merge)?\\s*"
  val r2 = "(explain\\s+)?create\\s*\\(.*\\{?.*\\}?\\s*\\)"
  val pattern = new Regex(s"${r1}|${r2}|${r3}")

  val pandaConfig = PandaRuntimeContext.contextGet[PandaConfig]()

  def isWriteStatement(statement: String): Boolean = {
    val cypher = statement.toLowerCase().replaceAll("\n", "").replaceAll("\r", "")
    !pattern.findAllIn(cypher).isEmpty
  }

  def isWriteOnLeader(): Boolean = {
    if (pandaConfig.useJraft) {
      val pandaService = PandaRuntimeContext.contextGet[PandaJraftService]()
      if (pandaService.isLeader()) {
        true
      } else false
    } else true
  }
}
