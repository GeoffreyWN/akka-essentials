package com.akkaEssentials.faultTolerance

import akka.actor.SupervisorStrategy.{ Escalate, Restart, Resume, Stop }
import akka.actor.{ Actor, ActorRef, ActorSystem, AllForOneStrategy, OneForOneStrategy, Props, SupervisorStrategy, Terminated }
import akka.testkit.{ EventFilter, ImplicitSender, TestKit }
import org.scalatest.BeforeAndAfterAll
import org.scalatest.wordspec.AnyWordSpecLike

import scala.util.control

class SupervisionSpec
    extends TestKit(ActorSystem("SupervisionSpec"))
    with ImplicitSender
    with AnyWordSpecLike
    with BeforeAndAfterAll {

  override def afterAll(): Unit = {
    TestKit.shutdownActorSystem(system)
  }

  import SupervisionSpec._

  "A supervisor " should {
    "resume it's child in case of a minor fault" in {
      val supervisor = system.actorOf(Props[Supervisor])
      supervisor ! Props[FussyWordCounter]
      val child      = expectMsgType[ActorRef]

      child ! "I love AKKA WORLD"
      child ! Report
      expectMsg(4)

      child ! "I love AKKA WORLD I love AKKA WORLD I love AKKA WORLD I love AKKA WORLD I love AKKA WORLD I love AKKA WORLD I love AKKA WORLD. Actor model Rocks!"
      child ! Report
      expectMsg(4)
    }

    "restart it's child in case of an empty sentence" in {
      val supervisor = system.actorOf(Props[Supervisor])
      supervisor ! Props[FussyWordCounter]
      val child      = expectMsgType[ActorRef]

      child ! ""
      child ! Report
      expectMsg(
        0
      ) // the strategy for a NullPointerException is to restart hence all the actors state is destroyed as the actor instance inside the actor ref is swapped

    }

    "terminate it's child in case of a major error" in {
      val supervisor = system.actorOf(Props[Supervisor])
      supervisor ! Props[FussyWordCounter]
      val child      = expectMsgType[ActorRef]

      watch(child)

      child ! "akka is nice."

      val terminatedMessage = expectMsgType[Terminated]
      assert(terminatedMessage.actor == child)

    }

    "escalate an error when it doesnt know what to do" in {
      val supervisor = system.actorOf(Props[Supervisor], "supervisor")
      supervisor ! Props[FussyWordCounter]
      val child      = expectMsgType[ActorRef]

      watch(child)

      child ! 100

      val terminatedMessage = expectMsgType[Terminated]
      assert(terminatedMessage.actor == child)
    }
  }

  "A kinder supervisor" should {
    "not kill children in case it's restarted or escalates failures" in {
      val supervisor = system.actorOf(Props[NoDeathOnRestartSupervisor], "supervisor2")
      supervisor ! Props[FussyWordCounter]
      val child      = expectMsgType[ActorRef]

      child ! "Akka is awesome"
      child ! Report
      expectMsg(3)

      child ! 350
      child ! Report
      expectMsg(0)

    }
  }

  "An all for one supervisor" should {
    "apply all-for-one-strategy" in {
      val supervisor = system.actorOf(Props[AllForOneSupervisor], "allforOneSupervisor")
      supervisor ! Props[FussyWordCounter]
      val childOne   = expectMsgType[ActorRef]

      supervisor ! Props[FussyWordCounter]
      val childTwo = expectMsgType[ActorRef]

      childTwo ! "Cool test"
      childTwo ! Report
      expectMsg(2)

      EventFilter[NullPointerException]() intercept {
        childOne ! ""
      }

      Thread.sleep(500)

      childTwo ! Report
      expectMsg(0)

    }
  }


}

object SupervisionSpec {

  class Supervisor extends Actor {

    override val supervisorStrategy: SupervisorStrategy = OneForOneStrategy() {
      case _: NullPointerException     => Restart
      case _: IllegalArgumentException => Stop
      case _: RuntimeException         => Resume
      case _: Exception                => Escalate

    }

    override def receive: Receive = { case props: Props =>
      val childRef = context.actorOf(props)
      sender() ! childRef
    }
  }

  class NoDeathOnRestartSupervisor extends Supervisor {
    override def preRestart(reason: Throwable, message: Option[Any]): Unit = {
      // empty

    }
  }

  class AllForOneSupervisor extends Supervisor {
    override val supervisorStrategy = AllForOneStrategy() {
      case _: NullPointerException     => Restart
      case _: IllegalArgumentException => Stop
      case _: RuntimeException         => Resume
      case _: Exception                => Escalate
    }
  }

  case object Report

  class FussyWordCounter extends Actor {
    var words = 0

    override def receive: Receive = {
      case Report           => sender() ! words
      case ""               => throw new NullPointerException("sentence is empty")
      case sentence: String =>
        if (sentence.length > 20) throw new RuntimeException("sentence is too long")
        else if (!Character.isUpperCase(sentence(0))) throw new IllegalArgumentException("sentence must start with uppercase")
        else words += sentence.split(" ").length
      case _                => throw new Exception("can only receive strings")
    }
  }
}
