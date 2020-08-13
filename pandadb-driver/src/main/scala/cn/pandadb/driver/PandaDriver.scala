package cn.pandadb.driver

import java.util
import java.util.Collections
import java.util.concurrent.{CompletableFuture, CompletionStage}
import java.util.function.Consumer

import com.alipay.sofa.jraft.RouteTable
import org.apache.commons.lang3.NotImplementedException
import org.neo4j.driver.async.AsyncSession
import org.neo4j.driver.internal.{AbstractStatementRunner, SessionParameters}
import org.neo4j.driver.internal.metrics.{InternalMetricsProvider, MetricsProvider}
import org.neo4j.driver.internal.util.{Clock, Futures}
import org.neo4j.driver.reactive.RxSession
import org.neo4j.driver.types.TypeSystem
import org.neo4j.driver.{
  AuthToken, Config, Driver, Metrics, Record, Session, SessionParametersTemplate,
  Statement, StatementResult, Transaction, TransactionConfig, TransactionWork, Value, Values
}

object PandaDriver {
  def create(uri: String, authToken: AuthToken, config: Config): Driver = {
    val str = uri.split("//")
    new PandaDriver(str(1), authToken, config)
  }
}

class PandaDriver(uri: String, authToken: AuthToken, config: Config) extends Driver {
  val rt = RouteTable.getInstance()

  val defaultSessionConfig = SessionParameters.empty()

  override def closeAsync(): CompletionStage[Void] = {
    //TODO
    new CompletableFuture[Void]();
  }

  override def asyncSession(consumer: Consumer[SessionParametersTemplate]): AsyncSession = {
    throw new NotImplementedException("asyncSession")
  }

  override def session(): Session = {
    new PandaSession(authToken, defaultSessionConfig, rt, uri);
  }

  override def session(consumer: Consumer[SessionParametersTemplate]): Session = {
    throw new NotImplementedException("session consumer")
    //    new PandaSession(defaultSessionConfig, rt, uri);
  }


  //  def session(sessionConfig: SessionParameters): Session = new PandaSession(sessionConfig, rt, uri);

  override def rxSession(): RxSession = {
    this.rxSession(defaultSessionConfig)
  }

  def rxSession(sessionConfig: SessionParameters): RxSession = {
    throw new NotImplementedException("rxSession")
  }

  override def rxSession(consumer: Consumer[SessionParametersTemplate]): RxSession = {
    throw new NotImplementedException("rxSession")
  }

  override def metrics(): Metrics = {
    createDriverMetrics(config, this.createClock()).metrics()
  }

  private def createDriverMetrics(config: Config, clock: Clock): MetricsProvider = {
    if (config.isMetricsEnabled()) new InternalMetricsProvider(clock) else MetricsProvider.METRICS_DISABLED_PROVIDER
  }

  private def createClock(): Clock = {
    Clock.SYSTEM
  }

  override def asyncSession(): AsyncSession = {
    this.asyncSession(defaultSessionConfig)
  }

  def asyncSession(sessionConfig: SessionParameters): AsyncSession = {
    throw new NotImplementedException("asyncSession")
  }

  override def close(): Unit = {

  }

  //wait to finish
  override def isEncrypted: Boolean = {
    throw new NotImplementedException("isEncrypted")
  }

}

class PandaSession(authToken: AuthToken, sessionConfig: SessionParameters, routeTable: RouteTable, uri: String) extends Session {
  var session: Session = null
  var readDriver: Driver = null
  var writeDriver: Driver = null

  private def getSession(isWriteStatement: Boolean): Session = {
    if (!(this.session == null)) this.session.close()
    if (isWriteStatement) {
      if (this.writeDriver == null) this.writeDriver = SelectNode.getDriver(authToken, isWriteStatement, routeTable, uri)
      this.session = this.writeDriver.session()
    } else {
      if (this.readDriver == null) this.readDriver = SelectNode.getDriver(authToken, isWriteStatement, routeTable, uri)
      this.session = this.readDriver.session()
    }
    this.session
  }

  override def writeTransaction[T](work: TransactionWork[T]): T = {
    this.writeTransaction(work, TransactionConfig.empty())
  }

  override def writeTransaction[T](work: TransactionWork[T], config: TransactionConfig): T = {
    getSession(true).writeTransaction(work, config)
  }

  override def readTransaction[T](work: TransactionWork[T]): T = {
    this.readTransaction(work, TransactionConfig.empty())
  }

  override def readTransaction[T](work: TransactionWork[T], config: TransactionConfig): T = {
    getSession(false).readTransaction(work, config)
  }

  override def run(statement: String, config: TransactionConfig): StatementResult = {
    this.run(statement, Collections.emptyMap(), config)
  }

  override def run(statement: String, parameters: util.Map[String, AnyRef], config: TransactionConfig): StatementResult = {
    this.run(new Statement(statement, parameters), config)
  }

  override def run(statement: Statement, config: TransactionConfig): StatementResult = {
    val tempState = statement.text().toLowerCase()
    val isWriteStatement = DriverUtils.isWriteStatement(tempState)
    getSession(isWriteStatement)
    this.session.run(statement, config)
  }

  override def close(): Unit = {
    if (!(this.session == null)) session.close()
    if (!(this.writeDriver == null)) this.writeDriver.close()
    if (!(this.readDriver == null)) this.readDriver.close()
  }

  override def lastBookmark(): String = {
    session.lastBookmark()
  }

  override def reset(): Unit = {
    session.reset()
  }

  override def beginTransaction(): Transaction = {
    this.beginTransaction(TransactionConfig.empty())
  }

  override def beginTransaction(config: TransactionConfig): Transaction = {
    /*isTransaction = true
    this.config = config
    this.transaction*/
    new PandaTransaction(authToken, sessionConfig, config, routeTable, uri)
  }

  override def run(statementTemplate: String, parameters: Value): StatementResult = {
    this.run(new Statement(statementTemplate, parameters))
  }

  override def run(statementTemplate: String, statementParameters: util.Map[String, AnyRef]): StatementResult = {
    this.run(statementTemplate, AbstractStatementRunner.parameters(statementParameters))
  }

  override def run(statementTemplate: String, statementParameters: Record): StatementResult = {
    //session.run(statementTemplate, statementParameters) AbstractStatementRunner
    //this.run(statementTemplate, parameters(statementParameters))
    this.run(statementTemplate, AbstractStatementRunner.parameters(statementParameters))
  }

  override def run(statementTemplate: String): StatementResult = {
    this.run(statementTemplate, Values.EmptyMap)
  }

  override def run(statement: Statement): StatementResult = {
    //session.run(statement)
    this.run(statement, TransactionConfig.empty())
  }

  override def isOpen: Boolean = {
    session.isOpen
  }

  override def typeSystem(): TypeSystem = {
    throw new NotImplementedException("typeSystem")
  }
}
