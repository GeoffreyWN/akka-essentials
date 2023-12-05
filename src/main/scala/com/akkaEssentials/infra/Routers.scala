package com.akkaEssentials.infra

import akka.actor.{ Actor, ActorLogging, ActorSystem, Props, Terminated }
import akka.routing.{ ActorRefRoutee, Broadcast, FromConfig, RoundRobinGroup, RoundRobinPool, RoundRobinRoutingLogic, Router }
import com.typesafe.config.ConfigFactory

object Routers extends App {

  /** #1 Manual router
    */

  class Patron extends Actor with ActorLogging {
    // create routees
    private val workers = for (i <- 1 to 5) yield {
      val worker = context.actorOf(Props[Worker], s"worker_$i")
      context.watch(worker)
      ActorRefRoutee(worker)
    }

    // define router
    private val router = Router(RoundRobinRoutingLogic(), workers)

    override def receive: Receive = {
      // handle the lifecycle of the routees
      case Terminated(ref) =>
        router.removeRoutee(ref)
        val newWorker = context.actorOf(Props[Worker])
        context.watch(newWorker)
        router.addRoutee(newWorker)

      // route the messages
      case message         => router.route(message, sender())
    }

  }

  class Worker extends Actor with ActorLogging {
    override def receive: Receive = { case message =>
      log.info(message.toString)
    }

  }

  val system = ActorSystem("RouterSDemo", ConfigFactory.load().getConfig("routersDemo"))

  val patron = system.actorOf(Props[Patron])

//  for (i <- 1 to 10) {
//    patron ! s"[$i] Let get busy!"
//  }

  /** #2 a router with it's own children
    * POOL Router
    */

  // programmatically
  val poolPatron = system.actorOf(RoundRobinPool(5).props(Props[Worker]), "simplePoolMaster")

//    for (i <- 1 to 10) {
//      poolPatron ! s"[$i] Let get busy!"
//    }

  // from configuration
  val poolPatron2 = system.actorOf(FromConfig.props(Props[Worker]), "poolPatron2")

//  for (i <- 1 to 10) {
//    poolPatron2 ! s"[$i] Let get busy!"
//  }

  /** #3 a Router with actors created elsewhere
    * GROUP Router
    */

  val workersList = (1 to 5).map(i => system.actorOf(Props[Worker], s"worker_$i")).toList

  val workerPaths = workersList.map(workerRef => workerRef.path.toString)

  val groupPatron = system.actorOf(RoundRobinGroup(workerPaths).props())

//      for (i <- 1 to 10) {
//        groupPatron ! s"[$i] Let get busy!"
//      }

  val groupPatron2 = system.actorOf(FromConfig.props(), "groupPatron2")

  for (i <- 1 to 10) {
    groupPatron2 ! s"[$i] Let get busy group 2!"
  }

  /**
   * Special Messages
    */

  groupPatron2 ! Broadcast("Hey Peeps")

  // PoisonPill and Kill are NOT routed
  //AddRoutee, Remove get handled only by the routing actor
}
