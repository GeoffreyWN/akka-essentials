package com.akkaEssentials.Actors

import akka.actor.{ Actor, ActorLogging, ActorRef, ActorSystem, Props }

object Parent {
  case class CreateChild(name: String)
  case class TellChild(message: String)
}

class Parent extends Actor with ActorLogging {
  import Parent._

  override def receive: Receive = { case CreateChild(name) =>
    log.info(s"${self.path} creating child")
    val childRef = context.actorOf(Props[Child], name)
    context.become(withChild(childRef))
  }

  def withChild(childRef: ActorRef): Receive = { case TellChild(message) =>
    if (childRef != null) childRef forward message
  }
}

class Child extends Actor with ActorLogging {
  override def receive: Receive = { case message =>
    log.info(s"${self} ${self.path.name} I got: $message")
  }
}

object ChildActors extends App {
  import Parent._
  val system = ActorSystem("ParentChild-Sys")

  val parentActor = system.actorOf(Props[Parent], "parentActor")

  parentActor ! CreateChild("coolKidActor")
  parentActor ! TellChild("Hello kiddo!")

  val childSelection = system.actorSelection("/user/parentActor/coolKidActor")

  childSelection ! "Amazing! You found me!"
}
