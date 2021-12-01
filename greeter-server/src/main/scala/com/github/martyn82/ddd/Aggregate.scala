package com.github.martyn82.ddd

trait Aggregate {
  protected val applyCommand: CommandHandler[_, _, _]
  protected val applyEvent: EventHandler[_, _]
}
