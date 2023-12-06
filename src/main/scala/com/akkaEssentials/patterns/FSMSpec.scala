package com.akkaEssentials.patterns

import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, Cancellable, FSM, Props}
import akka.testkit.{ImplicitSender, TestKit}
import org.scalatest.{BeforeAndAfterAll, OneInstancePerTest}
import org.scalatest.wordspec.AnyWordSpecLike

import scala.concurrent.ExecutionContextExecutor
import scala.concurrent.duration.DurationInt
import scala.language.postfixOps

class FSMSpec extends TestKit(ActorSystem("FSMSpec")) with ImplicitSender with AnyWordSpecLike with BeforeAndAfterAll with OneInstancePerTest {
  override def afterAll(): Unit = {
    TestKit.shutdownActorSystem(system)
  }

  import FSMSpec._
  "A vending machine" should {
    runTestSuite(Props[VendingMachine])
  }

  "A vending machine FSM" should {
    runTestSuite(Props[VendingMachineFSM])
  }

  private def runTestSuite(props: Props): Unit = {
    "error when not initialized" in {
      val vendingMachine = system.actorOf(props)
      vendingMachine ! RequestProduct("peanuts")

      expectMsg(VendingError("MachineNotInitialized"))
    }

    "report a product not available" in {
      val vendingMachine = system.actorOf(props)

      vendingMachine ! Initialize(Map("peanuts" -> 20), Map("peanuts" -> 100))
      vendingMachine ! RequestProduct("burger")
      expectMsg(VendingError("ProductNotAvailable"))
    }

    "throw a timeout if I don't insert money" in {
      val vendingMachine = system.actorOf(props)

      vendingMachine ! Initialize(Map("peanuts" -> 20), Map("peanuts" -> 100))
      vendingMachine ! RequestProduct("peanuts")
      expectMsg(Instruction("Please insert Ksh 100"))

      within(1500 millis) {
        expectMsg(VendingError("RequestTimedOut"))
      }
    }

    "handle the reception of partial money" in {
      val vendingMachine = system.actorOf(props)
      vendingMachine ! Initialize(Map("peanuts" -> 20), Map("peanuts" -> 100))
      vendingMachine ! RequestProduct("peanuts")
      expectMsg(Instruction("Please insert Ksh 100"))

      vendingMachine ! ReceiveMoney(10)
      expectMsg(Instruction("Please insert Ksh 90"))

      within(1500 millis) {
        expectMsg(VendingError("RequestTimedOut"))
        expectMsg(GiveBackChange(10))
      }
    }

    "deliver the product if all the money is inserted" in {
      val vendingMachine = system.actorOf(props)
      vendingMachine ! Initialize(Map("peanuts" -> 20), Map("peanuts" -> 100))
      vendingMachine ! RequestProduct("peanuts")
      expectMsg(Instruction("Please insert Ksh 100"))

      vendingMachine ! ReceiveMoney(100)
      expectMsg(Deliver("peanuts"))
    }

    "give back change and be able to request money for a new product" in {
      val vendingMachine = system.actorOf(props)
      vendingMachine ! Initialize(Map("peanuts" -> 20), Map("peanuts" -> 100))
      vendingMachine ! RequestProduct("peanuts")
      expectMsg(Instruction("Please insert Ksh 100"))

      vendingMachine ! ReceiveMoney(105)
      expectMsg(Deliver("peanuts"))
      expectMsg(GiveBackChange(5))

      vendingMachine ! RequestProduct("peanuts")
      expectMsg(Instruction("Please insert Ksh 100"))

    }
  }
}

object FSMSpec {

  /** Vending machine
    */

  case class Initialize(inventory: Map[String, Int], prices: Map[String, Int])
  case class RequestProduct(product: String)

  case class Instruction(instruction: String)
  case class ReceiveMoney(amount: Int)
  case class Deliver(product: String)
  case class GiveBackChange(amount: Int)

  case class VendingError(reason: String)
  case object ReceiveMoneyTimeout

  class VendingMachine extends Actor with ActorLogging {
    implicit val executionContext: ExecutionContextExecutor = context.dispatcher
    override def receive: Receive                           = idle

    def idle: Receive = {
      case Initialize(inventory, prices) => context.become(operational(inventory, prices))
      case _                             => sender() ! VendingError("MachineNotInitialized")
    }

    def operational(inventory: Map[String, Int], prices: Map[String, Int]): Receive = { case RequestProduct(product) =>
      inventory.get(product) match {
        case None | Some(0) =>
          sender() ! VendingError("ProductNotAvailable")
        case Some(_)        =>
          val price = prices(product)
          sender() ! Instruction(s"Please insert Ksh $price")
          context.become(waitForMoney(inventory, prices, product, 0, startReceiveMoneyTimeoutSchedule, sender()))
      }
    }

    def waitForMoney(
      inventory: Map[String, Int],
      prices: Map[String, Int],
      product: String,
      money: Int,
      moneyTimeoutSchedule: Cancellable,
      requester: ActorRef
    ): Receive = {
      case ReceiveMoneyTimeout =>
        requester ! VendingError("RequestTimedOut")
        if (money > 0) requester ! GiveBackChange(money)
        context.become(operational(inventory, prices))

      case ReceiveMoney(amount) =>
        moneyTimeoutSchedule.cancel()
        val price = prices(product)
        if (money + amount >= price) {
          requester ! Deliver(product)

          if (money + amount - price > 0) requester ! GiveBackChange(money + amount - price)
          val newStock     = inventory(product) - 1
          val newInventory = inventory + (product -> newStock)
          context.become(operational(newInventory, prices))
        }
        else {
          val remainingMoney = price - money - amount
          requester ! Instruction(s"Please insert Ksh $remainingMoney")
          context.become(waitForMoney(inventory, prices, product, money + amount, startReceiveMoneyTimeoutSchedule, requester))

        }
    }

    def startReceiveMoneyTimeoutSchedule: Cancellable = context.system.scheduler.scheduleOnce(1 second) {
      self ! ReceiveMoneyTimeout
    }

  }

  //FSM:  define states and data of the actor
  trait VendingState
  case object Idle extends VendingState
  case object Operational extends VendingState
  case object WaitForMoney extends VendingState

  trait VendingData
  case object Uninitialized extends VendingData
  case class Initialized(inventory: Map[String, Int], prices: Map[String, Int]) extends VendingData
  case class WaitForMoneyData(
    inventory: Map[String, Int],
    prices: Map[String, Int],
    product: String,
    money: Int,
    requester: ActorRef
  ) extends VendingData

  class VendingMachineFSM extends FSM[VendingState, VendingData] {
    // when an FSM receives a message it triggers an event, the event contains the message and data

    startWith(Idle, Uninitialized)

    // takes a partial function that handles events
    when(Idle) {
      case Event(Initialize(inventory, prices), Uninitialized) =>
        goto(Operational) using Initialized(inventory, prices)
      case _                                                   =>
        sender() ! VendingError("MachineNotInitialized")
        stay()
    }

    when(Operational) {
      case Event(RequestProduct(product), Initialized(inventory, prices)) =>
        inventory.get(product) match {
          case None | Some(0) =>
            sender() ! VendingError("ProductNotAvailable")
            stay()
          case Some(_) =>
            val price = prices(product)
            sender() ! Instruction(s"Please insert Ksh $price")
            goto(WaitForMoney) using WaitForMoneyData(inventory, prices, product, 0, sender())

        }

    }

    when(WaitForMoney, stateTimeout = 1 second) {
      case Event(StateTimeout, WaitForMoneyData(inventory,prices,product, money, requester)) =>
        requester ! VendingError("RequestTimedOut")
        if (money > 0) requester ! GiveBackChange(money)
        goto(Operational) using Initialized(inventory, prices)
      case Event(ReceiveMoney(amount), WaitForMoneyData(inventory,prices,product, money, requester)) =>
        val price = prices(product)
        if (money + amount >= price) {
          requester ! Deliver(product)

          if (money + amount - price > 0) requester ! GiveBackChange(money + amount - price)
          val newStock = inventory(product) - 1
          val newInventory = inventory + (product -> newStock)
          goto(Operational) using Initialized(newInventory, prices)
        }
        else {
          val remainingMoney = price - money - amount
          requester ! Instruction(s"Please insert Ksh $remainingMoney")
          stay() using WaitForMoneyData(inventory, prices, product, money + amount, requester)
        }
     }

    whenUnhandled{
      case Event(_,_) =>
        sender() ! VendingError("CommandNotFound")
        stay()
    }

    onTransition{
      case stateA -> stateB => log.info(s"Transition from $stateA to $stateB")
    }

    initialize()

  }

}
