package com.akkaEssentials.testing

import akka.actor.{ Actor, ActorRef, ActorSystem, Props }
import akka.testkit.{ ImplicitSender, TestKit, TestProbe }
import org.scalatest.BeforeAndAfterAll
import org.scalatest.wordspec.AnyWordSpecLike

class TestProbeSpec
    extends TestKit(ActorSystem("TestProbeSpec"))
    with ImplicitSender
    with AnyWordSpecLike
    with BeforeAndAfterAll {

  override def afterAll(): Unit = {
    TestKit.shutdownActorSystem(system)
  }

  import TestProbeSpec._

  "A MainActor" should {
    "register a worker" in {
      val mainActor   = system.actorOf(Props[MainActor])
      val workerActor = TestProbe("workerActor")

      mainActor ! Register(workerActor.ref)
      expectMsg(RegistrationAck)
    }

    "send work to the WorkerActor" in {
      val mainActor   = system.actorOf(Props[MainActor])
      val workerActor = TestProbe("workerActor")

      mainActor ! Register(workerActor.ref)
      expectMsg(RegistrationAck)
      val workLoadString = "We are going to the scala conference! Hurray"
      mainActor ! Work(workLoadString)

      // interaction between the MainActor and the  WorkerActor
      workerActor.expectMsg(WorkerWork(workLoadString, testActor))
      workerActor.reply(WorkCompleted(8, testActor))

      expectMsg(Report(8)) // testActor receives the Report(8) message
    }

    "aggregate data correctly" in {
      val mainActor   = system.actorOf(Props[MainActor])
      val workerActor = TestProbe("workerActor")

      mainActor ! Register(workerActor.ref)
      expectMsg(RegistrationAck)

      val workLoadString = "We should probably pass by the Scala conference first, right guys?"
      mainActor ! Work(workLoadString)
      mainActor ! Work(workLoadString)

      workerActor.receiveWhile() { case WorkerWork(`workLoadString`, `testActor`) =>
        workerActor.reply(WorkCompleted(11, testActor))
      }

      expectMsg(Report(11))
      expectMsg(Report(22))

    }

  }

}

object TestProbeSpec {

  case class Register(workerRef: ActorRef)
  case object RegistrationAck
  case class Work(text: String)
  case class WorkerWork(text: String, originalRequester: ActorRef)
  case class WorkCompleted(count: Int, originalRequester: ActorRef)
  case class Report(totalCount: Int)

  class MainActor extends Actor {
    override def receive: Receive = {
      case Register(workerRef) =>
        sender() ! RegistrationAck
        context.become(online(workerRef, 0))
      case _                   => //ignore
    }

    def online(workerRef: ActorRef, totalWordCount: Int): Receive = {
      case Work(text)                              => workerRef ! WorkerWork(text, sender())
      case WorkCompleted(count, originalRequester) =>
        val newTotalWordCount = totalWordCount + count
        originalRequester ! Report(newTotalWordCount)
        context.become(online(workerRef, newTotalWordCount))
    }

  }

}
