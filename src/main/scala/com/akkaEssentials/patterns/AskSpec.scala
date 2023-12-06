package com.akkaEssentials.patterns

import akka.actor.{ Actor, ActorLogging, ActorRef, ActorSystem, Props }
import akka.pattern.{ ask, pipe }
import akka.testkit.{ ImplicitSender, TestKit }
import akka.util.Timeout
import org.scalatest.BeforeAndAfterAll
import org.scalatest.wordspec.AnyWordSpecLike

import scala.concurrent.ExecutionContextExecutor
import scala.concurrent.duration.DurationInt
import scala.language.postfixOps
import scala.util.{ Failure, Success }

class AskSpec extends TestKit(ActorSystem("AskSpec")) with ImplicitSender with AnyWordSpecLike with BeforeAndAfterAll {

  override def afterAll(): Unit = {
    TestKit.shutdownActorSystem(system)
  }

  import AskSpec._

  "An authenticator " should {
    authenticatorTestSuite(Props[AuthManager])

  }

  "A piped authenticator " should {
    authenticatorTestSuite(Props[PipedAuthManager])
  }

  private def authenticatorTestSuite(props: Props): Unit = {
    import AuthManager._
    "fail to authenticate an unregistered user" in {
      val authManager = system.actorOf(props)

      authManager ! Authenticate("thor", "marvel001")
      expectMsg(AuthFailure(AUTH_FAILURE_NOT_FOUND))
    }

    "fail to authenticate if an invalid password is provided" in {
      val authManager = system.actorOf(props)

      authManager ! RegisterUser("hulk", "marvel002")
      authManager ! Authenticate("hulk", "marvel007")
      expectMsg(AuthFailure(AUTH_FAILURE_INCORRECT_CREDENTIALS))
    }

    "successfully authenticate a registered user" in {
      val authManager = system.actorOf(props)

      authManager ! RegisterUser("hulk", "marvel002")
      authManager ! Authenticate("hulk", "marvel002")
      expectMsg(AuthSuccess)
    }
  }

}

object AskSpec {
  case class Read(key: String)
  case class Write(key: String, value: String)

  class KVActor extends Actor with ActorLogging {

    override def receive: Receive = online(Map())

    def online(kv: Map[String, String]): Receive = {
      case Read(key)         =>
        log.info(s"Trying to read the value at key: $key")
        sender() ! kv.get(key)
      case Write(key, value) =>
        log.info(s"Writing the value: $value for key: $key")
        context.become(online(kv + (key -> value)))
    }
  }

  // Auth Actor

  case class RegisterUser(username: String, password: String)
  case class Authenticate(username: String, password: String)
  case class AuthFailure(message: String)
  case object AuthSuccess

  object AuthManager {
    val AUTH_FAILURE_NOT_FOUND             = "Username not found"
    val AUTH_FAILURE_INCORRECT_CREDENTIALS = "Incorrect credentials"
    val AUTH_FAILURE_SYSTEM_ERROR          = "Sorry, error occurred!"
  }

  class AuthManager extends Actor with ActorLogging {
    import AuthManager._

    implicit val timeout: Timeout                           = Timeout(1 second)
    implicit val executionContext: ExecutionContextExecutor = context.dispatcher

    protected val authDb: ActorRef = context.actorOf(Props[KVActor])

    override def receive: Receive = {
      case RegisterUser(username, password) => authDb ! Write(username, password)
      case Authenticate(username, password) => handleAuth(username, password)

    }

    def handleAuth(username: String, password: String): Unit = {
      val currentSender = sender()
      val future        = authDb ? Read(username)

      future onComplete {
        case Success(None)             => currentSender ! AuthFailure(AUTH_FAILURE_NOT_FOUND)
        case Success(Some(dbPassword)) =>
          if (dbPassword == password) currentSender ! AuthSuccess
          else currentSender ! AuthFailure(AUTH_FAILURE_INCORRECT_CREDENTIALS)
        case Failure(_)                => currentSender ! AuthFailure(AUTH_FAILURE_SYSTEM_ERROR)
      }
    }
  }

  class PipedAuthManager extends AuthManager {
    import AuthManager._

    override def handleAuth(username: String, password: String): Unit = {
      val future         = authDb ? Read(username)
      val passwordFuture = future.mapTo[Option[String]]
      val responseFuture = passwordFuture.map {
        case None             => AuthFailure(AUTH_FAILURE_NOT_FOUND)
        case Some(dbPassword) =>
          if (dbPassword == password) AuthSuccess
          else AuthFailure(AUTH_FAILURE_INCORRECT_CREDENTIALS)
      }

      responseFuture.pipeTo(sender())

    }

  }

}
