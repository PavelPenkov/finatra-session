# finatra-session #

Work in progress support for sessions in Finatra. Session is `Map[String, String]` , it's encrypted with `PBEWithMD5AndDES` (1000 key stretch iterations) and signed with `HmacSHA1`.

## Usage ##

In server definition

	import me.penkov.finatra.session._
	
	object MyModule extends TwitterModule {
			@Provides
			def providesSessionFilter = new SessionFilter(System.getenv("APP_SECRET"), "my_cookie_name")	
	}

    class ExampleServer extends HttpServer {
		override def configureHttp(router: HttpRouter) {
	    	router
		      .add[SessionFilter,HomeController]
	  }
	}

 

In controller

	import me.penkov.finatra.session.SessionContext
	
	class HomeController extends Controller {
		get("/") { req: Request =>
			val key = req.session.getOrElse("csrf_token", "")
			response.ok("hello").session(Map("key" -> "value"))
		}
	}