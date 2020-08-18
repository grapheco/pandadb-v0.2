package cn.pandadb.driver

import java.util

import com.alipay.sofa.jraft.RouteTable
import org.apache.commons.lang3.NotImplementedException
import org.neo4j.driver.internal.{AbstractStatementRunner, SessionParameters}
import org.neo4j.driver.types.TypeSystem
import org.neo4j.driver.{AuthToken, Driver, Record, Session, Statement, StatementResult, StatementRunner, Transaction, TransactionConfig, Value, Values}

import scala.collection.mutable.ArrayBuffer


class PandaTransaction(authToken: AuthToken, sessionConfig: SessionParameters, config: TransactionConfig, routeTable: RouteTable, uri: String) extends Transaction {

  var transactionArray: ArrayBuffer[Transaction] = ArrayBuffer[Transaction]() //save session
  var sessionArray: ArrayBuffer[Session] = ArrayBuffer[Session]() // save transaction

  var transaction: Transaction = _
  var writeTransaction: Transaction = _
  var session: Session = _
  var readDriver: Driver = _
  var writeDriver: Driver = _

  private def getSession(isWriteStatement: Boolean): Session = {
    if (isWriteStatement) {
      if (this.writeDriver == null) this.writeDriver = SelectNode.getDriver(authToken, isWriteStatement, routeTable, uri)
      this.session = this.writeDriver.session()
    } else {
      if (this.readDriver == null) this.readDriver = SelectNode.getDriver(authToken, isWriteStatement, routeTable, uri)
      this.session = this.readDriver.session()
    }
    this.session
  }

  private def getTransactionReady(isWriteStatement: Boolean): Transaction = {
    if (!(this.writeTransaction == null)) this.transaction = this.writeTransaction //reuse the wrtie transaction
    else {
      this.session = getSession(isWriteStatement)
      this.transaction = session.beginTransaction(config)
      if (isWriteStatement) this.writeTransaction = this.transaction
      this.sessionArray += this.session
      this.transactionArray += this.transaction
    }
    this.transaction

  }

  override def success(): Unit = {
    if (this.transactionArray.nonEmpty) this.transactionArray.foreach(trans => trans.success())
  }

  override def failure(): Unit = {
    if (this.transactionArray.nonEmpty) this.transactionArray.foreach(trans => trans.failure())
  }

  override def close(): Unit = {
    if (this.transactionArray.nonEmpty) this.transactionArray.foreach(
      trans => try {
        trans.close()
      } catch {
        case ex: Exception => {
          trans.failure()
          trans.close()
          throw ex
        }
      }
    )
    if (this.sessionArray.nonEmpty) this.sessionArray.foreach(sess => sess.close())
    if (!(this.writeDriver == null)) this.writeDriver.close()
    if (!(this.readDriver == null)) this.readDriver.close()
  }

  override def run(s: String, value: Value): StatementResult = {
    this.run(new Statement(s, value))
  }

  override def run(s: String, map: util.Map[String, AnyRef]): StatementResult = {
    this.run(s, AbstractStatementRunner.parameters(map))
  }

  override def run(s: String, record: Record): StatementResult = {
    this.run(s, AbstractStatementRunner.parameters(record))
  }

  override def run(s: String): StatementResult = {
    this.run(s, Values.EmptyMap)
  }

  override def run(statement: Statement): StatementResult = {
    //transanction could not be closed until close function
    val tempState = statement.text().toLowerCase()
    val isWriteStatement = DriverUtils.isWriteStatement(tempState)
    getTransactionReady(isWriteStatement)
    this.transaction.run(statement)
  }

  override def isOpen: Boolean = {
    if (!(this.transaction == null)) this.transaction.isOpen else true
  }

  override def typeSystem(): TypeSystem = {
    throw new NotImplementedException("typeSystem")
  }
}


