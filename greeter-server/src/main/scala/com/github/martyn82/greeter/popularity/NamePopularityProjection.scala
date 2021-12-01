package com.github.martyn82.greeter.popularity

import akka.actor.typed.ActorSystem
import akka.cluster.sharding.typed.ShardedDaemonProcessSettings
import akka.cluster.sharding.typed.scaladsl.ShardedDaemonProcess
import akka.persistence.query.Offset
import akka.persistence.query.journal.leveldb.scaladsl.LeveldbReadJournal
import akka.projection.cassandra.scaladsl.CassandraProjection
import akka.projection.eventsourced.EventEnvelope
import akka.projection.eventsourced.scaladsl.EventSourcedProvider
import akka.projection.scaladsl.{AtLeastOnceProjection, SourceProvider}
import akka.projection.{ProjectionBehavior, ProjectionId}
import com.github.martyn82.greeter.Greeter
import com.github.martyn82.greeter.Greeter.Event

import scala.concurrent.Await
import scala.concurrent.duration._

object NamePopularityProjection {
  def init(system: ActorSystem[_], repository: NamePopularityRepository): Unit = {
    Await.result(repository.init(), 20 seconds)

    ShardedDaemonProcess(system).init(
      name = "NamePopularityProjection",
      numberOfInstances = Greeter.tags.size,
      index => ProjectionBehavior(createProjectionFor(system, repository, index)),
      ShardedDaemonProcessSettings(system),
      Some(ProjectionBehavior.Stop)
    )
  }

  private def createProjectionFor(system: ActorSystem[_], repository: NamePopularityRepository, index: Int): AtLeastOnceProjection[Offset, EventEnvelope[Event]] = {
    val tag = Greeter.tags(index)

    val sourceProvider: SourceProvider[Offset, EventEnvelope[Event]] = EventSourcedProvider.eventsByTag[Event](
      system = system,
      readJournalPluginId = LeveldbReadJournal.Identifier,
      tag = tag
    )

    val handler = new NamePopularityProjectionHandler(tag, system, repository)

    CassandraProjection.atLeastOnce(
      projectionId = ProjectionId("NamePopularityProjection", tag),
      sourceProvider,
      handler = () => handler
    )
  }
}
