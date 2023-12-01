package com.akkaEssentials.faultTolerance

import akka.actor.{ Actor, ActorLogging, ActorRef, ActorSystem, Kill, PoisonPill, Props, Terminated }
import com.akkaEssentials.faultTolerance.StartingStoppingActors.Parent.StartChild

object StartingStoppingActors extends App {
  val system = ActorSystem("stopping-actors")

  object Parent {
    case class StartChild(name: String)
    case class StopChild(name: String)
    case object Stop
  }

  class Parent extends Actor with ActorLogging {
    import Parent._
    override def receive: Receive = withChildren(Map())

    def withChildren(children: Map[String, ActorRef]): Receive = {
      case StartChild(name) =>
        log.info(s"Starting child $name")
        context.become(withChildren(children + (name -> context.actorOf(Props[Child], name))))
      case StopChild(name)  =>
        log.info(s"Stopping child with the name: $name")
        val childOption = children.get(name)
        println(s"children => $children")
        childOption foreach (childRef => context.stop(childRef))
      case Stop             =>
        log.info("Stopping myself")
        context.stop(self)
      case message          =>
        log.info(message.toString)
    }
  }

  class Child extends Actor with ActorLogging {
    override def receive: Receive = { case message =>
      log.info(message.toString)
    }
  }

  /** 1. stopping an actor using context.stop
    */
//
//  import Parent._
//  val parent   = system.actorOf(Props[Parent], "parent")
//  parent ! StartChild("childOne")
//  val childOne = system.actorSelection("/user/parent/childOne")
//
//  childOne ! "hey there, childOne!"
//  parent ! StopChild("childOne")
////  for(i <- 1 to 20) childOne ! s"[$i] childOne are you still with us?"
//
//  parent ! StartChild("childTwo")
//  val childTwo = system.actorSelection("/user/parent/childTwo")
//  childTwo ! "hey there, childTwo!"
//
//  parent ! Stop
//
//  for (i <- 1 to 10) parent ! s"[$i] parent are you still with us?"
//  for (i <- 1 to 100) childTwo ! s"[$i] childTwo are you still with us?"

  /** 2. stopping an actor using special messages
    */

//  val looseActor = system.actorOf(Props[Child])
//
//  looseActor ! "hey loose actor"
//  looseActor ! PoisonPill
//  looseActor ! "looseActor Are you still there?"
//
//  val abruptlyTerminatedActor = system.actorOf(Props[Child])
//  abruptlyTerminatedActor ! "About to be terminated"
//  abruptlyTerminatedActor ! Kill
//  looseActor ! "abruptlyTerminatedActor you have been terminated?"

  /**  Death watch
    */

  class Watcher extends Actor with ActorLogging {
    import Parent._
    override def receive: Receive = {
      case StartChild(name) =>
        val child = context.actorOf(Props[Child], name)
        log.info(s"Started and watching child $name")
        context.watch(child)
      case Terminated(ref)  =>
        log.info(s"the actorRef that I am watching ($ref) has been stopped!")
    }
  }

  val watcherActor = system.actorOf(Props[Watcher], "watcher")

  watcherActor ! StartChild("watchedChild")

  val watchedChild = system.actorSelection("/user/watcher/watchedChild")
  Thread.sleep(500)

  watchedChild ! PoisonPill


}
