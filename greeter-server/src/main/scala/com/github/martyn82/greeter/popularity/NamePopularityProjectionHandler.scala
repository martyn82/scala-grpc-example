package com.github.martyn82.greeter.popularity

import akka.Done
import akka.actor.typed.ActorSystem
import akka.projection.eventsourced.EventEnvelope
import akka.projection.scaladsl.Handler
import com.github.martyn82.greeter.Greeter._

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Success

object NamePopularityProjectionHandler {
  val LogInterval = 10
}

class NamePopularityProjectionHandler(tag: String, system: ActorSystem[_], repo: NamePopularityRepository) extends Handler[EventEnvelope[Event]] {
  private implicit val ec: ExecutionContext = system.executionContext

  def init(): Future[Done] =
    repo.init()

  override def process(envelope: EventEnvelope[Event]): Future[Done] = {
    val processed = envelope.event match {
      case SaidHello(name, _) =>
        repo.update(name, 1)
    }

    processed.onComplete {
      case Success(_) =>
        println("Popularity updated")

      case _ => ()
    }
    processed
  }
}
