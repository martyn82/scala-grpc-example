package com.github.martyn82.greeter.publishing

import akka.actor.typed.ActorSystem
import akka.cluster.sharding.typed.ShardedDaemonProcessSettings
import akka.cluster.sharding.typed.scaladsl.ShardedDaemonProcess
import akka.persistence.query.Offset
import akka.persistence.query.journal.leveldb.scaladsl.LeveldbReadJournal
import akka.projection.{ProjectionBehavior, ProjectionId}
import akka.projection.cassandra.scaladsl.CassandraProjection
import akka.projection.eventsourced.EventEnvelope
import akka.projection.eventsourced.scaladsl.EventSourcedProvider
import akka.projection.scaladsl.AtLeastOnceProjection
import com.github.martyn82.greeter.Greeter
import com.github.martyn82.greeter.Greeter.Event
import com.sksamuel.pulsar4s.PulsarClient

object EventPublisherProjection {
  def init(system: ActorSystem[_], client: PulsarClient): Unit = {
    val topic = "persistent://public/default/greeter"

    ShardedDaemonProcess(system).init(
      name = "EventPublisherProjection",
      Greeter.tags.size,
      index =>
        ProjectionBehavior(
          createProjectionFor(system, topic, client, index)
        ),
      ShardedDaemonProcessSettings(system),
      Some(ProjectionBehavior.Stop)
    )
  }

  private def createProjectionFor(system: ActorSystem[_], topic: String, client: PulsarClient, index: Int):
    AtLeastOnceProjection[Offset, EventEnvelope[Event]] = {
    val tag = Greeter.tags(index)

    val sourceProvider = EventSourcedProvider.eventsByTag[Event](
      system = system,
      readJournalPluginId = LeveldbReadJournal.Identifier,
      tag = tag
    )

    val handler = new EventPublisherProjectionHandler(topic, client)

    CassandraProjection.atLeastOnce(
      projectionId = ProjectionId("EventPublisherProjection", tag),
      sourceProvider,
      handler = () => handler
    )
  }
}
