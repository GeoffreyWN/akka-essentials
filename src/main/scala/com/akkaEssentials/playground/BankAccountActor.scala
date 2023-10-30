package com.akkaEssentials.playground

import akka.actor.{ Actor, ActorLogging, ActorRef, ActorSystem, Props }

//
object BankAccountActor {
  case class Deposit(amount: Int)
  case class Withdraw(amount: Int)
  case object Statement
  case class TransactionSuccess(message: String)
  case class TransactionFailure(reason: String)
}

class BankAccountActor extends Actor {
  import BankAccountActor._

  var balance = 0

  override def receive: Receive = {
    case Deposit(amount: Int)  =>
      if (amount < 0)
        sender() ! TransactionFailure("Invalid deposit amount")
      else {
        balance += amount
        sender() ! TransactionSuccess(s"Deposit of amount:($amount) was successful!")
      }
    case Withdraw(amount: Int) =>
      if (amount < 0)
        sender() ! TransactionFailure("Invalid withdraw amount")
      else if (amount > balance) sender() ! TransactionFailure("Insufficient funds")
      else {
        balance -= amount
        sender() ! TransactionSuccess(s"Successfully withdrew $amount !")
      }

    case Statement => sender() ! TransactionSuccess(s"Your account balance is: $balance")

  }

}

object PersonActor {
  case class SavingsAccount(account: ActorRef)
}

class PersonActor extends Actor with ActorLogging {
  import PersonActor._
  import BankAccountActor._

  override def receive: Receive = {
    case SavingsAccount(account) =>
      account ! Deposit(1000)
      account ! Withdraw(5000)
      account ! Withdraw(100)
      account ! Statement

    case message => log.info(message.toString)
  }
}

object MainBankApp extends App {
  import PersonActor._
  val system = ActorSystem("Bank-Sys")

  val accountActor = system.actorOf(Props[BankAccountActor], "bankAccountActor")
  val personActor  = system.actorOf(Props[PersonActor], "BigManKiongos")

  personActor ! SavingsAccount(accountActor)

}
