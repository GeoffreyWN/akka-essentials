package com.akkaEssentials.faultTolerance

import akka.actor.SupervisorStrategy.Stop
import akka.actor.{Actor, ActorLogging, ActorSystem, OneForOneStrategy, Props}
import akka.pattern.BackoffOpts.{onFailure, onStop}
import akka.pattern.BackoffSupervisor

import java.io.File
import scala.concurrent.duration.DurationInt
import scala.io.Source
import scala.language.postfixOps

object BackoffSupervisorPattern extends App {
  case object ReadFile

  class FileBasedPersistentActor extends Actor with ActorLogging {
    var dataSource: Source = null

    override def preStart(): Unit = log.info("Persistent Actor starting")

    override def postStop(): Unit = log.info("Persistent Actor has stopped")

    override def preRestart(reason: Throwable, message: Option[Any]): Unit = log.warning("Persistent Actor restarting")

    override def receive: Receive = { case ReadFile =>
      if (dataSource == null) {
        dataSource = Source.fromFile(new File("src/main/resources/testfiles/important_data.txt"))
        log.info("I've just read some important data:" + dataSource.getLines().toList)
      }
    }
  }

  val system = ActorSystem("BackoffSupervisorDemo")
//  val simpleActor = system.actorOf(Props[FileBasedPersistentActor], "simpleActor")
//
//  simpleActor ! ReadFile

  val simpleSupervisorProps = BackoffSupervisor.props(
    onFailure(
      Props[FileBasedPersistentActor],
      "simpleBackoffActor",
      3 seconds,
      30 seconds,
      0.2
    )
  )

//  val simpleBacloffSupervisor = system.actorOf(simpleSupervisorProps, "simpleSupervisor")
//  simpleBacloffSupervisor ! ReadFile

  val stopSupervisorProps = BackoffSupervisor.props(
    onStop(
      Props[FileBasedPersistentActor],
      "stopBackoffActor",
      3 seconds,
      30 seconds,
      0.2
    ).withSupervisorStrategy(
      OneForOneStrategy() {
        case _ => Stop
      }
    )
  )

//  val stopSupervisor = system.actorOf(simpleSupervisorProps, "stopSupervisor")
//  stopSupervisor ! ReadFile

  class EagerFileBasedPersistenceActor extends FileBasedPersistentActor {
    override def preStart(): Unit =
      log.info("Eager actor starting")
    dataSource = Source.fromFile(new File("src/main/resources/testfiles/important_data.txt"))
  }

  val eagerActor = system.actorOf(Props[EagerFileBasedPersistenceActor])

  val repeatedSupervisorProps = BackoffSupervisor.props(
    onStop(
      Props[EagerFileBasedPersistenceActor],
      "eagerActor",
      1 seconds,
      30 seconds,
      0.1
    )
  )

    val repeatedSupervisor = system.actorOf(repeatedSupervisorProps, "eagerSupervisor")



}
