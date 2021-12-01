package com.github.martyn82.greeter.popularity

import akka.Done
import akka.stream.alpakka.cassandra.scaladsl.CassandraSession

import scala.concurrent.{ExecutionContext, Future}

object NamePopularityRepositoryImpl {
  val Keyspace = "akka_projection"
  val PopularityTable = "name_popularity"
}

class NamePopularityRepositoryImpl(session: CassandraSession)(implicit val ec: ExecutionContext) extends NamePopularityRepository {
  import NamePopularityRepositoryImpl._

  override def init(): Future[Done] = {
    session.executeDDL(
      s"""
      |  CREATE KEYSPACE IF NOT EXISTS $Keyspace
      |    WITH REPLICATION = { 'class' : 'SimpleStrategy','replication_factor':1 };
      """.stripMargin
    )

    session.executeDDL(
      s"""
      |  USE $Keyspace ;
      """.stripMargin
    )

    session.executeDDL(
      s"""
      |  CREATE TABLE IF NOT EXISTS $Keyspace.projection_management (
      |    projection_name text,
      |    partition int,
      |    projection_key text,
      |    paused boolean,
      |    last_updated timestamp,
      |    PRIMARY KEY ((projection_name, partition), projection_key));
      """.stripMargin
    )

    session.executeDDL(
      s"""
      |  CREATE TABLE IF NOT EXISTS $Keyspace.$PopularityTable (
      |    name text,
      |    count counter,
      |    PRIMARY KEY (name));
      |""".stripMargin
    )

    session.executeDDL(
      s"""
      |CREATE TABLE IF NOT EXISTS $Keyspace.offset_store (
      |  projection_name text,
      |  partition int,
      |  projection_key text,
      |  offset text,
      |  manifest text,
      |  last_updated timestamp,
      |  PRIMARY KEY ((projection_name, partition), projection_key));
      |""".stripMargin
    )
  }

  override def update(name: String, delta: Int): Future[Done] = {
    println(s"GOT $name")

    session.executeWrite(
      s"UPDATE $Keyspace.$PopularityTable SET count = count + ? WHERE name = ?",
      java.lang.Long.valueOf(delta),
      name
    )
  }

  override def getName(name: String): Future[Option[Long]] = {
    session
      .selectOne(
        s"SELECT count FROM $Keyspace.$PopularityTable WHERE name = ?",
        name
      )
      .map(opt => opt.map(row => row.getLong("count").longValue()))
  }
}
