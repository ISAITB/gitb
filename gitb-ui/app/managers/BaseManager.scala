package managers

import play.api.Play
import play.api.db.slick.DatabaseConfigProvider
import slick.dbio.DBIO
import slick.jdbc.JdbcProfile

import scala.concurrent.Await
import scala.concurrent.duration.Duration

/**
 * Created by serbay on 10/22/14.
 */
abstract class BaseManager {

	val dbConfig = DatabaseConfigProvider.get[JdbcProfile](Play.current)
	val DB = dbConfig.db

	final def exec[R](a: DBIO[R]): R = {
		Await.result(DB.run(a), Duration.Inf)
	}

}
