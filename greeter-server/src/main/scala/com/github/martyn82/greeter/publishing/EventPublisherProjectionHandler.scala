package com.github.martyn82.greeter.publishing

import akka.Done
import akka.projection.eventsourced.EventEnvelope
import akka.projection.scaladsl.Handler
import com.github.martyn82.greeter.Greeter.Event
import com.sksamuel.pulsar4s.{ProducerConfig, ProducerMessage, PulsarClient, Topic}
import org.apache.pulsar.client.api.Schema

import scala.concurrent.Future

class EventPublisherProjectionHandler(topic: String, client: PulsarClient) extends Handler[EventEnvelope[Event]] {
  private implicit val schema: Schema[String] = Schema.STRING

  private lazy val producer = client.producer(
    ProducerConfig(
      Topic(topic)
    )
  )

  override def process(envelope: EventEnvelope[Event]): Future[Done] = {
    val message =
      s"""{
       | persistenceId: ${envelope.persistenceId},
       | offset: ${envelope.offset},
       | sequenceNr: ${envelope.sequenceNr},
       | timestamp: ${envelope.timestamp},
       | event: ${envelope.event}
       |}""".stripMargin

    println(s"Send $message")

    producer.send(
      ProducerMessage[String](message)
    ).toEither.fold(
      e => Future.failed(e),
      _ => Future.successful(Done)
    )
  }
}
