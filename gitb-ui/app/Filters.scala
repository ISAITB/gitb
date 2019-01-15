import filters.{AuthenticationFilter, CorsFilter, ErrorFilter, TimeoutFilter}
import javax.inject.Inject
import play.api.http.DefaultHttpFilters

class Filters @Inject() (
  cors: CorsFilter,
  error: ErrorFilter,
  auth: AuthenticationFilter,
  timeout: TimeoutFilter
) extends DefaultHttpFilters(cors, error, auth, timeout)
