package com.github.martyn82.greeter

import akka.Done
import akka.actor.typed.{ActorRef, ActorSystem, Behavior}
import akka.cluster.sharding.typed.scaladsl.{ClusterSharding, Entity, EntityTypeKey}
import akka.pattern.StatusReply
import akka.persistence.typed.PersistenceId
import akka.persistence.typed.scaladsl.{Effect, EventSourcedBehavior, Recovery, ReplyEffect}
import com.github.martyn82.ddd._

object Greeter extends Aggregate {
  val EntityKey: EntityTypeKey[Command] = EntityTypeKey[Command]("Greeter")

  sealed trait Command
  final case class SayHello(name: String, replyTo: ActorRef[StatusReply[Done]]) extends Command

  sealed trait Event extends Serializable
  final case class SaidHello(name: String, message: String) extends Event

  object State {
    val Empty: State = State(Nil)
  }
  final case class State(history: List[String])

  val tags: Vector[String] = Vector.tabulate(1)(i => s"greeter-$i")

  def init(system: ActorSystem[_]): Unit = {
    println("Entity initialization")

    ClusterSharding(system).init(Entity(EntityKey) { entityContext =>
      Greeter(entityContext.entityId, tags(0))
    })
  }

  def apply(id: String, projectionTag: String): Behavior[Command] =
    EventSourcedBehavior
      .withEnforcedReplies(
        persistenceId   = PersistenceId(EntityKey.name, id),
        emptyState      = State.Empty,
        commandHandler  = Greeter.applyCommand,
        eventHandler    = Greeter.applyEvent
      )
      .withTagger(_ => Set(projectionTag))
      .withRecovery(Recovery.default)

  override protected val applyCommand: CommandHandler[Command, Event, State] = { (state, command) =>
    println("Command received")

    command match {
      case command: SayHello =>
        sayHello(state, command)
    }
  }

  override protected val applyEvent: EventHandler[Event, State] = { (state, event) =>
    println("Event received")

    event match {
      case event: SaidHello =>
        onSaidHello(state, event)
    }
  }

  private def sayHello(state: State, command: SayHello): ReplyEffect[Event, State] = {
    println("Received command SayHello")

    Effect
      .persist(SaidHello(command.name, s"Hello, ${command.name}"))
      .thenReply(command.replyTo)(_ => StatusReply.Ack)
  }

  private def onSaidHello(state: State, event: SaidHello): State = {
    println("Received event SaidHello")
    state.copy((event.message :: state.history).take(5))
  }
}
