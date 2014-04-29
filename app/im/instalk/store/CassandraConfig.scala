package im.instalk.store

import scala.collection.JavaConverters._
import com.datastax.driver.core.ConsistencyLevel

trait CassandraConfig {
  this: CassandraPersistence =>

  val contactPoints = config.getStringList("contact-points").
    getOrElse(throw config.reportError("contact-points", "undefined cassandra contact points")).
    asScala

  val keyspace = config.getString("keyspace").
    getOrElse(throw config.reportError("keyspace", "keyspace is not defined"))

  val table = config.getString("table").
    getOrElse(throw config.reportError("table", "table is not defined"))

  val limit = config.getInt("limit").
    getOrElse(throw config.reportError("limit", "limit factor is not defined"))

  val replicationFactor = config.getInt("replication-factor").
    getOrElse(throw config.reportError("replication-factor", "replication factor is not defined"))

  val writeConsistency = ConsistencyLevel.valueOf(config.getString("write-consistency").
    getOrElse(throw config.reportError("write-consistency", "write-consistency is not defined")))

  val readConsistency = ConsistencyLevel.valueOf(config.getString("read-consistency").
    getOrElse(throw config.reportError("read-consistency", "read-consistency is not defined")))

}