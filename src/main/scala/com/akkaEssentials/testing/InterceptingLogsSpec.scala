package com.akkaEssentials.testing

import akka.actor.{ Actor, ActorLogging, ActorSystem, Props }
import akka.testkit.{ EventFilter, ImplicitSender, TestKit }
import com.typesafe.config.ConfigFactory
import org.scalatest.BeforeAndAfterAll
import org.scalatest.wordspec.AnyWordSpecLike

class InterceptingLogsSpec
    extends TestKit(ActorSystem("InterceptingLogsSpec", ConfigFactory.load().getConfig("interceptingLogMessages")))
    with ImplicitSender
    with AnyWordSpecLike
    with BeforeAndAfterAll {

  override def beforeAll(): Unit = {
    println("I will run before all other tests")
  }

  override def afterAll(): Unit = {
    println("I will run after all other tests")
    TestKit.shutdownActorSystem(system)
  }

  import InterceptingLogsSpec._

  val item = "Nike Sneaker 007"
  val validCreditCard = "1234-1234-1234-1234"
  val invalidCreditCard = "0000-0000-0000-0000"

  "A checkout flow" should {

    "correctly log the dispatch of an order" in {
      EventFilter.info(pattern = s"Order [0-9]+ for item $item has been dispatched!", occurrences = 1) intercept {
        // test code goes here:
        val checkoutRef = system.actorOf(Props[CheckoutActor])
        checkoutRef ! CheckoutItem(item, validCreditCard)
      }
    }

    "freak out of payment is denied" in {
      EventFilter[RuntimeException](occurrences = 1) intercept {
        val checkoutRef = system.actorOf(Props[CheckoutActor])
        checkoutRef ! CheckoutItem(item, invalidCreditCard)
      }
    }
  }

}

object InterceptingLogsSpec {

  case class CheckoutItem(item: String, card: String)
  case class AuthorizeCard(card: String)
  case object PaymentAccepted
  case object PaymentDenied
  case class DispatchOrder(item: String)
  case object OrderConfirmed

  class CheckoutActor extends Actor {

    private val paymentManager     = context.actorOf(Props[PaymentManager])
    private val fulfillmentManager = context.actorOf(Props[FulfillmentManager])

    override def receive: Receive = awaitingCheckout

    def awaitingCheckout: Receive = { case CheckoutItem(item, card) =>
      paymentManager ! AuthorizeCard(card)
      context.become(pendingPayment(item))
    }

    def pendingPayment(item: String): Receive = {
      case PaymentAccepted =>
        fulfillmentManager ! DispatchOrder(item)
        context.become(pendingFulfillment(item))
      case PaymentDenied   => throw new RuntimeException("Sorry!, I could not process payment")
    }

    def pendingFulfillment(item: String): Receive = { case OrderConfirmed =>
      context.become(awaitingCheckout)
    }
  }

  class PaymentManager extends Actor {
    override def receive: Receive = { case AuthorizeCard(card) =>
      // simulate complex card authorization logic
      if (card.startsWith("0")) sender() ! PaymentDenied
      else {
//        Thread.sleep(4000)
        sender() ! PaymentAccepted
      }
    }
  }

  class FulfillmentManager extends Actor with ActorLogging {
    var orderId                   = 0
    override def receive: Receive = { case DispatchOrder(item: String) =>
      orderId += 1
      log.info(s"Order $orderId for item $item has been dispatched!")
      sender() ! OrderConfirmed
    }
  }

}
