package cn.pandadb.jraft.snapshot

import java.io.File
import java.nio.file.Paths

import org.apache.commons.io.FileUtils
import org.apache.commons.lang.StringUtils

class LogIndexFile(path: String) {

  def getFile: File = {
    val fileDir = new File(path)
    if (!fileDir.exists()) {
      fileDir.mkdirs()
    }
    val file = new File(getPath + File.separator + "index")
    if (!file.exists()) {
      file.createNewFile()
    }
    file
  }

  def getPath: String = this.path
  def save(index: Int): Boolean = {
    try {
      FileUtils.writeStringToFile(getFile, index.toString)
      true
    }
    catch {
      case _: Exception => false
    }
  }

  def load(): Int = {
    val s = FileUtils.readFileToString(getFile)
    if (!StringUtils.isBlank(s)) s.toInt
    else Int.MinValue
  }
}
