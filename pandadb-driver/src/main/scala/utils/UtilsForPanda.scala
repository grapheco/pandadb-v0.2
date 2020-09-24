package utils

import scala.util.matching.Regex

class UtilsForPanda {
  val r1 = "(explain\\s)?match\\s*\\(.*\\s*\\{?.*\\}?\\s*\\)\\s*(where)?\\s*.*\\s*(set|remove|delete|merge)\\s*"
  val r3 = "(explain\\s+)?merge\\s*\\(.*\\s*\\{?.*\\}?\\s*\\)\\s*(where)?\\s*.*\\s*(set|remove|delete|merge)?\\s*"
  val r2 = "(explain\\s+)?create\\s*\\(.*\\{?.*\\}?\\s*\\)"
  val pattern = new Regex(s"${r1}|${r2}|${r3}")

  def isWriteStatement(statement: String): Boolean = {
    val cypher = statement.toLowerCase().replaceAll("\n", "").replaceAll("\r", "")
    val res = pattern.findAllIn(cypher)
    !res.isEmpty
  }
}
