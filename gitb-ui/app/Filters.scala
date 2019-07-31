import filters._
import javax.inject.Inject
import org.pac4j.play.filters.SecurityFilter
import play.api.http.DefaultHttpFilters

class Filters @Inject() (
  cors: CorsFilter,
  error: ErrorFilter,
  auth: AuthenticationFilter,
  timeout: TimeoutFilter,
  securityFilter: SecurityFilter
) extends DefaultHttpFilters(securityFilter, cors, error, auth, timeout)
