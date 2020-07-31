/*
 * Copyright (c) 2002-2019 "Neo4j,"
 * Neo4j Sweden AB [http://neo4j.com]
 *
 * This file is part of Neo4j.
 *
 * Neo4j is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package cn.pandadb.util

import java.io.File

/**
  * Created by bluejoe on 2019/7/23.
  */
trait Configuration {
  def getRaw(name: String): Option[String];
}

/**
  * Created by bluejoe on 2018/11/3.
  */
class ConfigurationOps(conf: Configuration) extends Logging {
  def getRequiredValueAsString(key: String): String = {
    getRequiredValue(key, (x) => x);
  }

  def getRequiredValueAsInt(key: String): Int = {
    getRequiredValue(key, (x) => x.toInt);
  }

  def getRequiredValueAsBoolean(key: String): Boolean = {
    getRequiredValue(key, (x) => x.toBoolean);
  }

  private def getRequiredValue[T](key: String, convert: (String) => T)(implicit m: Manifest[T]): T = {
    getValueWithDefault(key, () => throw new ArgumentRequiredException(key), convert);
  }

  private def getValueWithDefault[T](key: String, defaultValue: () => T, convert: (String) => T)(implicit m: Manifest[T]): T = {
    val opt = conf.getRaw(key);
    if (opt.isEmpty) {
      val value = defaultValue();
      logger.debug(s"no value set for $key, using default: $value");
      value;
    }
    else {
      val value = opt.get;
      try {
        convert(value);
      }
      catch {
        case e: java.lang.IllegalArgumentException =>
          throw new WrongArgumentException(key, value, m.runtimeClass);
      }
    }
  }

  def getValueAsString(key: String, defaultValue: String): String =
    getValueWithDefault(key, () => defaultValue, (x: String) => x)

  def getValueAsClass(key: String, defaultValue: Class[_]): Class[_] =
    getValueWithDefault(key, () => defaultValue, (x: String) => Class.forName(x))

  def getValueAsInt(key: String, defaultValue: Int): Int =
    getValueWithDefault[Int](key, () => defaultValue, (x: String) => x.toInt)

  def getValueAsBoolean(key: String, defaultValue: Boolean): Boolean =
    getValueWithDefault[Boolean](key, () => defaultValue, (x: String) => x.toBoolean)

  def getAsFile(key: String, baseDir: File, defaultValue: File): File = {
    getValueWithDefault(key, () => defaultValue, { x =>
      val file = new File(x);
      if (file.isAbsolute) {
        file
      }
      else {
        new File(baseDir, x)
      }
    });
  }
}

class ArgumentRequiredException(key: String) extends
  RuntimeException(s"argument required: $key") {

}

class WrongArgumentException(key: String, value: String, clazz: Class[_]) extends
  RuntimeException(s"wrong argument: $key, value=$value, expected: $clazz") {

}

object ConfigUtils {
  implicit def configOps(conf: Configuration): ConfigurationOps = new ConfigurationOps(conf);

  implicit def mapOps(map: Map[String, String]): ConfigurationOps = new ConfigurationOps(map2Config(map));

  implicit def contextMapOps(conf: ContextMap): ConfigurationOps = new ConfigurationOps(conf.toConfiguration);

  implicit def map2Config(map: Map[String, String]): Configuration = new Configuration() {
    override def getRaw(name: String): Option[String] = map.get(name)
  }
}