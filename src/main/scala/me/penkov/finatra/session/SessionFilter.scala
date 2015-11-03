package me.penkov.finatra.session

import com.twitter.finagle._
import com.twitter.finagle.http._
import com.twitter.finatra.http.response.ResponseBuilder
import com.twitter.util.Future
import java.io._
import me.penkov.crypto._
import me.penkov.utils.Utils._

class SessionFilter(secret: String, cookieName: String) extends SimpleFilter[Request, Response] {
  val encPool = new MessageEncryptorPool(secret)

  override def apply(req: Request, service: Service[Request, Response]): Future[Response] = {
    readSession(req)

    service(req).map { resp =>
      writeSession(resp)
      resp
    }
  }

  private def readSession(req: Request): Unit = {
    val s = req.cookies.get(cookieName).map { cookie =>
      val is = new ByteArrayInputStream(encPool.get.decryptAndVerify(cookie.value))
      using(new ObjectInputStream(is)) { ois =>
        ois.readObject.asInstanceOf[Map[String, String]]
      }
    } getOrElse Map.empty[String, String]
    req.ctx.update(SessionContext.ReqSessionField, s)
  }

  private def writeSession(resp: Response) = {
    val os = new ByteArrayOutputStream()
    val oos = new ObjectOutputStream(os)
    oos.writeObject(resp.ctx.apply(SessionContext.RespSessionField))
    oos.close()
    val s = encPool.get.encryptAndSign(os.toByteArray)
    resp.cookies.add(cookieName, new Cookie(cookieName, s))
  }
}

object SessionContext {
  val ReqSessionField = Request.Schema.newField[Map[String, String]](Map.empty[String, String])
  val RespSessionField = Response.Schema.newField[Map[String, String]](Map.empty[String, String])

  implicit class RequestExtensions(val req: Request) extends AnyVal {
    def session: Map[String, String] = req.ctx(ReqSessionField)
  }

  implicit class EnrichedResponseExtensions(val resp: ResponseBuilder#EnrichedResponse) extends AnyVal {
    def session(newSession: Map[String, String]): ResponseBuilder#EnrichedResponse = {
      resp.ctx.update(RespSessionField, newSession)
      resp
    }
  }

  implicit class ResponseExtensions(val resp: Response) extends AnyVal {
    def setSession(newSession: Map[String, String]): Unit = {
      resp.ctx.update(RespSessionField, newSession)
    }
  }
}
