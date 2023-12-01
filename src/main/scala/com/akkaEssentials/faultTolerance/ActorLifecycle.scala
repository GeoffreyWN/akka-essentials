package com.akkaEssentials.faultTolerance

import akka.actor.{ Actor, ActorLogging, ActorRef, ActorSystem, PoisonPill, Props }

object ActorLifecycle extends App {

  object StartChild

  class LifeCycleActor extends Actor with ActorLogging {
    override def receive: Receive = { case StartChild =>
      context.actorOf(Props[LifeCycleActor], "child")
    }

    override def preStart(): Unit = log.info("I am starting")
    override def postStop(): Unit = log.info("I have stopped ")

  }

  val system = ActorSystem("LifecycleDemo")
//  val parent = system.actorOf(Props[LifeCycleActor], "parent")
//  parent ! StartChild
//  parent ! PoisonPill

  /** Restart
    */

  object Fail
  object FailChild
  object CheckChild
  object Check

  class ParentActor extends Actor with ActorLogging {

    private val child: ActorRef = context.actorOf(Props[ChildActor], "supervisedChild")

    override def receive: Receive = {
      case FailChild  => child ! Fail
      case CheckChild => child ! Check
    }

  }

  class ChildActor extends Actor with ActorLogging {

    override def receive: Receive = {
      case Fail  =>
        log.warning("Child wil fail now")
        throw new RuntimeException("I failed ")
      case Check => log.warning("[Child] I am Alive and kicking!")

    }

    override def preStart(): Unit = log.info("supervisedChild started")

    override def postStop(): Unit = log.info("supervisedChild stopped ")

    override def preRestart(reason: Throwable, message: Option[Any]): Unit =
      log.info(s"supervisedChild actor restarting because of ${reason.getMessage}") // called by the old instance before restart

    override def postRestart(reason: Throwable): Unit =
      log.info(s"supervisedChild actor restarted ") // called by the new instance after restart

  }

  val supervisor = system.actorOf(Props[ParentActor], "supervisor")

  // default supervision startegy is to restart. (the message that caused the actor to throw an exception is removed from the queue and not put in the mailbox again)
  supervisor ! FailChild
  supervisor ! CheckChild

}
