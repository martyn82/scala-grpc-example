package com.github.martyn82.greeter

import akka.actor.ActorSystem
import akka.grpc.GrpcClientSettings
import com.github.martyn82.greeter.grpc.{GetPopularityRequest, GreeterServiceClient, SayHelloRequest}

import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success}

object Client {
  def main(args: Array[String]): Unit = {
    implicit val sys: ActorSystem = ActorSystem("Greeter")
    implicit val ec: ExecutionContext = sys.dispatcher

    val settings = GrpcClientSettings
      .connectToServiceAt("localhost", 50051)
      .withTls(false)

    val client = GreeterServiceClient(settings)

    println("SayHello")

    client.sayHello(
      SayHelloRequest.of(args(0))
    ).onComplete {
        case Success(value) =>
          println(value)

        case Failure(exception) =>
          println(exception.toString)
      }

    Thread.sleep(10000)
    println("GetPopularity")

    client.getPopularity(
      GetPopularityRequest.of("Foo")
    ).onComplete {
      case Success(value) =>
        println(value)

      case Failure(exception) =>
        println(exception.toString)
    }
  }
}
