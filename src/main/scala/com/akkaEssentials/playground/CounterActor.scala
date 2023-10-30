package com.akkaEssentials.playground

import akka.actor.{ Actor, ActorLogging, ActorSystem, Props }

object CounterActor {
  case class Increment(num: Int)
  case class Decrement(num: Int)
  case object Print
}

class CounterActor extends Actor with ActorLogging {
  import CounterActor._

  private var counter           = 0
  override def receive: Receive = {
    case Increment(num) => counter += num
    case Decrement(num) => counter -= num
    case Print          => log.info(s"Current Counter value is: $counter")
    case _              => println(s"Sorry, message unhandled")
  }
}

object Main extends App {
  val system: ActorSystem = ActorSystem("Counter-Sys")

  val counterActor = system.actorOf(Props[CounterActor], "counterActor")

  import CounterActor._
  counterActor ! Increment(10)
  counterActor ! Decrement(1)
  counterActor ! Print

}
