package com.github.martyn82.greeter

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.management.cluster.bootstrap.ClusterBootstrap
import akka.management.scaladsl.AkkaManagement
import akka.stream.alpakka.cassandra.scaladsl.CassandraSessionRegistry
import com.github.martyn82.greeter.popularity.{NamePopularityProjection, NamePopularityRepositoryImpl}
import com.github.martyn82.greeter.publishing.EventPublisherProjection
import com.github.martyn82.greeter.server.GreeterServer
import com.sksamuel.pulsar4s.{PulsarClient, PulsarClientConfig}
import com.typesafe.config.ConfigFactory

object Server {
  def main(args: Array[String]): Unit = {
    val conf = ConfigFactory
      .parseString("""
        | # General
        | akka.loglevel = WARNING
        |
        | # gRPC
        | akka.http.server.preview.enable-http2 = on
        |
        | # Cluster
        | akka.actor.provider = cluster
        |
        | akka.management.cluster.bootstrap.contact-point-discovery {
        |   service-name = "Greeter"
        |   discovery-method = config
        |   required-contact-point-nr = 1
        |   stable-margin = 1 ms
        |   contact-with-all-contact-points = false
        | }
        |
        | akka.discovery.config.services {
        |   "Greeter" {
        |     endpoints = [
        |       {host = "127.0.0.1", port = 9101}
        |     ]
        |   }
        | }
        |
        | akka.management.http.hostname = "127.0.0.1"
        | akka.management.http.port = 9101
        |
        | # Persistence
        | akka.persistence.journal.plugin = "akka.persistence.journal.leveldb"
        | akka.persistence.journal.leveldb.dir = "target/journal"
        |
        | # Serialization
        | akka.actor.allow-java-serialization = off
        | akka.actor.enable-additional-serialization-bindings = true
        |
        | akka.actor.serializers.jackson-json = "akka.serialization.jackson.JacksonJsonSerializer"
        |
        | akka.actor.serialization-bindings {
        |   "com.github.martyn82.ddd.Serializable" = jackson-json
        | }
        |
        | # Journal
        | akka.persistence.query.journal.leveldb {
        |   class = "akka.persistence.query.journal.leveldb.LeveldbReadJournalProvider"
        |   write-plugin = ""
        |   refresh-interval = 1s
        |   max-buffer-size = 100
        | }
        |
        | # Projection
        """.stripMargin
      )
      .withFallback(ConfigFactory.defaultApplication())

    val sys: ActorSystem[_] = ActorSystem[Nothing](Behaviors.empty, "GreeterService", conf)

    AkkaManagement(sys).start()
    ClusterBootstrap(sys).start()

    val session = CassandraSessionRegistry(sys)
      .sessionFor("akka.projection.cassandra.session-config")

    val repo = new NamePopularityRepositoryImpl(session)(sys.executionContext)
    NamePopularityProjection.init(sys, repo)

    val config = PulsarClientConfig("pulsar://localhost:6650")
    val client = PulsarClient(config)
    EventPublisherProjection.init(sys, client)

    Greeter.init(sys)

    val server = new GreeterServer("localhost", 50051, sys, repo)
    server.start()
  }
}
