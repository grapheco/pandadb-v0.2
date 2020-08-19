package concurrent

import org.junit.Assert

object ConcurrentTest {
  def main(args: Array[String]): Unit = {
    val d1 = new PandaDriver
    val d2 = new PandaDriver
    val d3 = new PandaDriver
    val d4 = new PandaDriver

    val testTimes = 5

    val lst = Array[PandaDriver](d1, d2, d3, d4)
    lst.par.foreach(
      driver => {
        driver.testMethod(testTimes)
      }
    )
    val d5 = new PandaDriver().driver
    val tx = d5.session().beginTransaction()
    val res = tx.run("match (n) return n")

    Assert.assertEquals(testTimes * 4, res.stream().count())

    tx.run("match (n) detach delete n")
    tx.success()
    tx.close()
    d5.close()
  }
}
