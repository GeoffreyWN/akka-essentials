package com.akkaEssentials.Actors

import akka.actor.{ Actor, ActorLogging, ActorRef, ActorSystem, Props }

object WordCounterMaster {
  case class Initialize(nChildren: Int)
  case class WordCountTask(id: Int, text: String)
  case class WordCountReply(id: Int, count: Int)
}

class WordCounterMaster extends Actor with ActorLogging {
  import WordCounterMaster._

  override def receive: Receive = { case Initialize(nChildren) =>
    log.info(s"${self.path.name} initializing...")
    val childrenRefs = for (i <- 1 to nChildren) yield context.actorOf(Props[WordCounterWorker], s"wcw_$i")
    context.become(withChildren(childrenRefs, 0, 0, Map()))
  }

  def withChildren(
    childrenRefs: Seq[ActorRef],
    currentChildIndex: Int,
    currentTaskId: Int,
    requestMap: Map[Int, ActorRef]
  ): Receive = {
    case text: String =>
      log.info(s"${self.path.name} I have received:$text. Sending it to child $currentChildIndex")
      val originalSender = sender()
      val task           = WordCountTask(currentTaskId, text)
      val childRef       = childrenRefs(currentChildIndex)
      childRef ! task
      val nextChildIndex = (currentChildIndex + 1)      % childrenRefs.length
      val newTaskId      = currentTaskId + 1
      val newRequestMap  = requestMap + (currentTaskId -> originalSender)
      context.become(withChildren(childrenRefs, nextChildIndex, newTaskId, newRequestMap))

    case WordCountReply(id, count) =>
      log.info(s"${self.path.name} ireceived a reply for task id $id with count: $count")
      val originalSender = requestMap(id)
      originalSender ! count
      context.become(withChildren(childrenRefs, currentChildIndex, currentTaskId, requestMap - id))

  }
}

class WordCounterWorker extends Actor with ActorLogging {
  import WordCounterMaster._

  override def receive: Receive = { case WordCountTask(id, text) =>
    log.info(s"${self.path.name} I have received task $id with $text")
    sender() ! WordCountReply(id, text.split(" ").length)
  }
}

class TestActor extends Actor with ActorLogging {
  import WordCounterMaster._
  override def receive: Receive = {
    case "go"       =>
      val master = context.actorOf(Props[WordCounterMaster], "masterActor")
      master ! Initialize(5)
      val texts  = List("We are going hiking!", "Would you like to come along?", "It will be awesome! See you at 0400 hours")
      texts.foreach(text => master ! text)
    case count: Int => log.info(s"${self.path.name} I received a reply: $count")
  }
}

object ChildActorsV2 extends App {

  val system = ActorSystem("WordCount-Sys")

  val testActor = system.actorOf(Props[TestActor], "test-actor")

  testActor ! "go"

}
