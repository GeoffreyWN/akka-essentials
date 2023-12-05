package com.akkaEssentials.infra

import akka.actor.{ Actor, ActorLogging, ActorSystem, Props }
import com.typesafe.config.ConfigFactory

import scala.concurrent.{ ExecutionContext, Future }
import scala.util.Random

object Dispatchers extends App {

  class Counter extends Actor with ActorLogging {
    var count = 0

    override def receive: Receive = { case message =>
      count += 1
      log.info(s"[$count] $message")
    }
  }

  val system = ActorSystem("DispatchersDemo", ConfigFactory.load().getConfig("dispatchersDemo"))

  // method #1 -programmatic
  val simpleCounterActors = for (i <- 1 to 10) yield system.actorOf(Props[Counter].withDispatcher("my-dispatcher"), s"counter_$i")

//  val rand = new Random()
//  for (i <- 1 to 100) {
//    simpleCounterActors(rand.nextInt(10)) ! i
//  }

  //method #2 - from config
  val counterActors = for (i <- 1 to 10) yield system.actorOf(Props[Counter], s"counterActor_$i")

//  val rand = new Random()
//  for (i <- 1 to 100) {
//    counterActors(rand.nextInt(10)) ! i
//  }

  /** Dispatchers implement the ExecutionContext Trait
    */

  class DBActor extends Actor with ActorLogging {
    // preferably use a dedicated dispatcher when executing futures within an actor rather than using the context.dispatcher. Frees up the dispatcher to handle actor messages
    // solution 1
    implicit val executionContext: ExecutionContext = context.system.dispatchers.lookup("my-dispatcher")


    override def receive: Receive                   = { case message =>
      Future {
        // wait on a resource
        Thread.sleep(5000)
        log.info(s"Success: $message")
      }
    }
  }

  val dbActor  = system.actorOf(Props[DBActor])

//  dbActor ! "Akka rocks!"

  val nonBlockingActor = system.actorOf(Props[Counter])

  for(i <- 1 to 1000) {
    val message = s"important message $i"
    dbActor ! message
    nonBlockingActor ! message
  }

}
