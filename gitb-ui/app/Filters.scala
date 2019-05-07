import filters._
import javax.inject.Inject
import play.api.http.DefaultHttpFilters

class Filters @Inject() (
  cors: CorsFilter,
  error: ErrorFilter,
  auth: AuthenticationFilter,
  timeout: TimeoutFilter
) extends DefaultHttpFilters(cors, error, auth, timeout)
