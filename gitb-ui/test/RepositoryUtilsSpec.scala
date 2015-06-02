import java.io.File

import org.specs2.mutable._
import org.specs2.runner._
import org.junit.runner._

import play.api.test._
import play.api.test.Helpers._
import utils.RepositoryUtils

/**
 * Created by serbay on 10/17/14.
 */
@RunWith(classOf[JUnitRunner])
class RepositoryUtilsSpec extends Specification {
	"RepositoryUtil" should {
		"extract test suite from zip file" in {
			val file = new File("gitb-ui/test/resources/SampleTestSuite.zip")

			file.exists must beTrue

			val suites = RepositoryUtils.getTestSuiteFromZip(0l, file)

			suites must beSome
		}

		"extract test suite files from zip file" in {
			val folder = new File("gitb/test/resources/repository")
			folder.mkdir()

			val file = new File("gitb/test/resources/SampleTestSuite.zip")

			RepositoryUtils.extractTestSuiteFilesFromZipToFolder(folder, file)

			val testSuiteFile = new File("gitb/test/resources/repository/SampleTestSuite.xml")

			testSuiteFile.exists must beTrue
		}
	}
}
