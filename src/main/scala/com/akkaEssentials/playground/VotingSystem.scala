package com.akkaEssentials.playground

import akka.actor.{ Actor, ActorLogging, ActorRef, ActorSystem, Props }

case class Vote(candidate: String)
case object VoteStatusRequest
case class VoteStatusReply(candidate: Option[String])

class Citizen extends Actor {
  override def receive: Receive = {
    case Vote(c)           => context.become(voted(c))
    case VoteStatusRequest => sender() ! VoteStatusReply(None)
  }

  def voted(candidate: String): Receive = { case VoteStatusRequest =>
    sender() ! VoteStatusReply(Some(candidate))
  }

}

case class AggregateVotes(citizens: Set[ActorRef])

class VoteAggregator extends Actor with ActorLogging {
//  var stillWaiting: Set[ActorRef] =  Set()
//  var currentStats: Map[String, Int] =  Map()

  override def receive: Receive = awaitingCommand

  def awaitingCommand: Receive = { case AggregateVotes(citizens) =>
    citizens.foreach(citizenRef => citizenRef ! VoteStatusRequest)
    context.become(awaitingStatuses(citizens, Map()))
  }

  def awaitingStatuses(stillWaiting: Set[ActorRef], currentStats: Map[String, Int]): Receive = {
    case VoteStatusReply(None)            => sender() ! VoteStatusRequest // citizen has not voted yet. (may result in an infinite loop)
    case VoteStatusReply(Some(candidate)) =>
      val newStillWaiting          = stillWaiting - sender()
      val currentVotesOfCandidates = currentStats.getOrElse(candidate, 0)
      val newStats                 = currentStats + (candidate -> (currentVotesOfCandidates + 1))
      if (newStillWaiting.isEmpty) {
        log.info(s"FINAL ELECTION RESULT: $newStats")
      }
      else {
        context.become(awaitingStatuses(newStillWaiting, newStats))
      }
  }

}

object CartoonNetworkApp extends App {
  val system = ActorSystem("Voting-Sys")

  val dexter      = system.actorOf(Props[Citizen])
  val kimPossible = system.actorOf(Props[Citizen])
  val courageDog  = system.actorOf(Props[Citizen])
  val tom         = system.actorOf(Props[Citizen])
  val jerry       = system.actorOf(Props[Citizen])

  dexter ! Vote("Hulk")
  kimPossible ! Vote("Thor")
  courageDog ! Vote("IronMan")
  tom ! Vote("Thor")
  jerry ! Vote("Dr. Strange")

  val voteAggregator = system.actorOf(Props[VoteAggregator])
  voteAggregator ! AggregateVotes(Set(dexter, kimPossible, courageDog, tom, jerry))

}
