import filters._
import javax.inject.Inject
import org.pac4j.play.filters.SecurityFilter
import play.api.http.DefaultHttpFilters

class Filters @Inject() (
  cors: CorsFilter,
  error: ErrorFilter,
  timeout: TimeoutFilter,
  securityFilter: SecurityFilter,
  auth: AuthenticationFilter,
  headerFilter: HeaderFilter
) extends DefaultHttpFilters(securityFilter, auth, cors, error, timeout, headerFilter)
