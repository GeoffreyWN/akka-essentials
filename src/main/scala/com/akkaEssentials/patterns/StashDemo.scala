package com.akkaEssentials.patterns

import akka.actor.{ Actor, ActorLogging, ActorSystem, Props, Stash }
import com.akkaEssentials.patterns.StashDemo.Open

object StashDemo extends App {
  case object Open
  case object Close
  case object Read
  case class Write(data: String)

  class ResourceActor extends Actor with ActorLogging with Stash {
    private var innerData: String = ""

    override def receive: Receive = closed

    def closed: Receive = {
      case Open    =>
        log.info("Opening Resource")
        unstashAll()
        context.become(open)
      case message =>
        log.info(s"Stashing $message because I am in the closed state")
        stash()
    }

    def open: Receive = {
      case Read        =>
        log.info(s"I have read $innerData")
      case Write(data) =>
        log.info(s"Writing data:  $data")
        innerData = data
      case Close       =>
        log.info("Closing resource")
        unstashAll()
        context.become(closed)
      case message     =>
        log.info(s"Stashing $message because I am in the closed state")
        stash()
    }

  }

  val system        = ActorSystem("StashDemo")
  val resourceActor = system.actorOf(Props[ResourceActor])

  resourceActor ! Write("Stash is very handy!")
  resourceActor ! Read
  resourceActor ! Open


}
