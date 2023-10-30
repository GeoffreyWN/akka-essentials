package com.akkaEssentials.Actors

import akka.actor.{ Actor, ActorLogging, ActorRef, ActorSystem, Props }

object Mom {
  case class MomStart(kidRef: ActorRef)
  case class Food(food: String)
  case class Ask(message: String)
  val VEGETABLE = "veggies"
  val CHOCOLATE = "chocolate"
}
class Mom extends Actor with ActorLogging {
  import Kid._
  import Mom._

  override def receive: Receive = {
    case MomStart(kidRef: ActorRef) =>
      kidRef ! Food(VEGETABLE)
      kidRef ! Food(VEGETABLE)
      kidRef ! Food(CHOCOLATE)
      kidRef ! Food(CHOCOLATE)
      kidRef ! Ask("Wanna play?")
    case KidAccept                  => log.info("Awesome kid is happy!")
    case KidReject                  => log.info("Kid is sad but healthy!! So we still celebrate")
  }
}

object Kid {
  case object KidAccept
  case object KidReject
  val HAPPY = "happy"
  val SAD   = "sad"
}
class Kid extends Actor {
  import Kid._
  import Mom._

  var state = HAPPY

  override def receive: Receive = {
    case Food(VEGETABLE) => state = SAD
    case Food(CHOCOLATE) => state = HAPPY
    case Ask(_)          =>
      if (state == HAPPY) sender() ! KidAccept
      else sender() ! KidReject
  }
}

class StatelessKid extends Actor {
  import Kid._
  import Mom._

  override def receive: Receive = happyReceive
  def happyReceive: Receive     = {
    case Food(VEGETABLE) => context.become(sadReceive, false) // change receive handler to sadReceive
    case Food(CHOCOLATE) => // stay happy
    case Ask(_)          => sender() ! KidAccept
  }
  def sadReceive: Receive       = {
    case Food(VEGETABLE) =>
      context.become(
        sadReceive,
        false
      ) // false value stacks the new handler on top of the old one // default true(replaces current handler)
    case Food(CHOCOLATE) => context.unbecome()
    case Ask(_)          => sender() ! KidReject
  }
}

object ChangingActorBehavior extends App {
  val system = ActorSystem("ChangeActorBehavior-Sys")

  val momActor           = system.actorOf(Props[Mom], "momActor")
  val kidActor           = system.actorOf(Props[Kid], "kidActor")
  val statelessKidkActor = system.actorOf(Props[StatelessKid], "statelessKidActor")

  import Mom._

//  momActor ! MomStart(kidActor)

  momActor ! MomStart(statelessKidkActor)

}
