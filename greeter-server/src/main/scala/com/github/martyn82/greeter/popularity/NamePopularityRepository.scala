package com.github.martyn82.greeter.popularity

import akka.Done

import scala.concurrent.Future

trait NamePopularityRepository {
  def init(): Future[Done]

  def update(name: String, delta: Int): Future[Done]

  def getName(name: String): Future[Option[Long]]
}
