import filters._
import org.pac4j.play.filters.SecurityFilter
import play.api.http.DefaultHttpFilters

import javax.inject.Inject

class Filters @Inject() (
  cors: CorsFilter,
  error: ErrorFilter,
  securityFilter: SecurityFilter,
  auth: AuthenticationFilter,
  headerFilter: HeaderFilter
) extends DefaultHttpFilters(securityFilter, auth, cors, error, headerFilter)
