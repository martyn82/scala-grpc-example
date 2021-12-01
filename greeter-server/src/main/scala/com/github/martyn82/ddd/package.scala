package com.github.martyn82

import akka.persistence.typed.scaladsl.ReplyEffect

package object ddd {
  type CommandHandler[C, E, S] = (S, C) => ReplyEffect[E, S]
  type EventHandler[E, S] = (S, E) => S
}
