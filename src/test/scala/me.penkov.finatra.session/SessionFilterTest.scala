package me.penkov.finatra.session

import com.twitter.finagle.Service
import com.twitter.finagle.http.{Cookie, Response, Request}
import com.twitter.util.{Await, Future, Duration}
import org.scalatest._
import org.scalatest.Matchers._

class SessionFilterTest extends FunSuite {
  val sessionFilter = new SessionFilter("test_secret", "finatra_test_session")
  val underlying = Service.mk[Request, Response] { request =>
    Future.value(request.response)
  }
  val service = sessionFilter andThen underlying

  test("should set cookie params") {
    val req = Request()
    val resp = Await.result(service(req))

    val cookie = resp.cookies("finatra_test_session")
    cookie should not be (null)
    cookie.name should equal ("finatra_test_session")
  }
}