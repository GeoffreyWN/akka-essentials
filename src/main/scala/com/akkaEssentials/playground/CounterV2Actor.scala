package com.akkaEssentials.playground

import akka.actor.{ Actor, ActorLogging, ActorSystem, Props }

object CounterV2Actor {
  case class Increment(num: Int)
  case class Decrement(num: Int)
  case object Print
}

class CounterV2Actor extends Actor with ActorLogging {
  import CounterV2Actor._

  override def receive: Receive = countReceive(0)

  def countReceive(currentCount: Int): Receive = {
    case Increment(num) =>
      log.info(s"[$currentCount] : Incrementing....")
      context.become(countReceive(currentCount + num))
    case Decrement(num) =>
      log.info(s"[$currentCount] : Decrementing....")
      context.become(countReceive(currentCount - num))
    case Print          => log.info(s"Current Counter value is: $currentCount")
    case _              => println(s"Sorry, message unhandled")
  }
}

object CounterV2App extends App {
  val system: ActorSystem = ActorSystem("CounterV2-Sys")

  val counterV2Actor = system.actorOf(Props[CounterV2Actor], "counterActor")

  import CounterV2Actor._
  (1 to 10).foreach(_ => counterV2Actor ! Increment(5))
  (1 to 5).foreach(_ => counterV2Actor ! Decrement(2))

  counterV2Actor ! Print

}
