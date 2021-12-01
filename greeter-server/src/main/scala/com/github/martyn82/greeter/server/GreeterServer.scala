package com.github.martyn82.greeter.server

import akka.actor.typed.ActorSystem
import akka.cluster.sharding.typed.scaladsl.ClusterSharding
import akka.grpc.GrpcServiceException
import akka.grpc.scaladsl.ServiceHandler
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{HttpRequest, HttpResponse}
import akka.stream.Materializer
import akka.util.Timeout
import com.github.martyn82.greeter.Greeter
import com.github.martyn82.greeter.grpc.{GetPopularityRequest, GetPopularityResponse, GreeterService, GreeterServiceHandler, SayHelloRequest, SayHelloResponse}
import com.github.martyn82.greeter.popularity.NamePopularityRepository
import io.grpc.Status

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future, TimeoutException}
import scala.util.{Failure, Success}

class GreeterServer(interface: String, port: Int, system: ActorSystem[_], repo: NamePopularityRepository) {
  def start(): Future[Http.ServerBinding] = {
    implicit val sys: ActorSystem[_] = system
    implicit val ec: ExecutionContext = system.executionContext
    implicit val timeout: Timeout = Timeout.durationToTimeout(5 seconds)

    val sharding = ClusterSharding(system)

    val service: HttpRequest => Future[HttpResponse] =
      ServiceHandler.concatOrNotFound(
        GreeterServiceHandler.partial(new GreeterServiceImpl(sharding, repo))
      )

    val bound: Future[Http.ServerBinding] = Http(system)
      .newServerAt(interface, port)
      .bind(service)
      .map(_.addToCoordinatedShutdown(hardTerminationDeadline = 10.seconds))

    bound.onComplete {
      case Success(binding) =>
        val address = binding.localAddress
        println("gRPC server running at " + address.getHostString + ":" + address.getPort)

      case Failure(ex) =>
        println("Failed to bind gRPC endpoint, terminating: " + ex)
        system.terminate()
    }
    bound
  }

  class GreeterServiceImpl(sharding: ClusterSharding, repository: NamePopularityRepository)(implicit val timeout: Timeout,
                                                      implicit val ec: ExecutionContext,
                                                      implicit val mat: Materializer) extends GreeterService {
    override def sayHello(request: SayHelloRequest): Future[SayHelloResponse] = {
      println("Got request: " + request.name)

      val greeter = sharding.entityRefFor(Greeter.EntityKey, "1")
      val reply = greeter.askWithStatus(Greeter.SayHello(request.name, _))

      convertError(
        reply.map(_ => SayHelloResponse.of("Helloooo"))
      )
    }

    override def getPopularity(request: GetPopularityRequest): Future[GetPopularityResponse] = {
      println("Got request: " + request)

      repository.getName(request.name).map {
        case Some(count) =>
          GetPopularityResponse.of(request.name, count)
        case None =>
          GetPopularityResponse.of(request.name, 0L)
      }
    }

    private def convertError[T](response: Future[T]): Future[T] =
      response.recoverWith {
        case _: TimeoutException =>
          Future.failed(
            new GrpcServiceException(
              Status.UNAVAILABLE.withDescription("Operation timed out")
            )
          )

        case ex =>
          Future.failed(
            new GrpcServiceException(
              Status.INVALID_ARGUMENT.withDescription(ex.getMessage)
            )
          )
      }
  }
}
