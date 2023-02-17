package db.migration.default

import org.apache.commons.io.FileUtils
import org.flywaydb.core.api.migration.{BaseJavaMigration, Context}
import org.slf4j.LoggerFactory

import java.io.{File, FilenameFilter}
import java.nio.file.{Files, Path}

class V74__Decouple_test_suite_folders_from_specs extends BaseJavaMigration {

  private def LOG = LoggerFactory.getLogger(classOf[V74__Decouple_test_suite_folders_from_specs])
  private def DEFAULT_ROOT_FOLDER = "/gitb-repository"

  override def migrate(context: Context): Unit = {
    val repositoryFolder: String = sys.env.get("TESTBED_REPOSITORY_PATH").orElse(Some(DEFAULT_ROOT_FOLDER)).get
    val rootFolderToUse = Path.of(repositoryFolder, "test-suites").toString

    var counter = 0
    val select = context.getConnection.createStatement
    try {
      val rs = select.executeQuery("SELECT `id`, `domain` FROM `specifications`")
      try {
        while (rs.next) {
          val specificationId = rs.getLong(1)
          val domainId = rs.getLong(2)
          val specificationFolder = Path.of(rootFolderToUse, domainId.toString, specificationId.toString)
          if (Files.exists(specificationFolder) && Files.isDirectory(specificationFolder)) {
            val children = Option(specificationFolder.toFile.listFiles(new FilenameFilter {
              override def accept(file: File, name: String): Boolean = {
                file.isDirectory && name.startsWith("ts_")
              }
            }))
            if (children.isDefined) {
              children.get.foreach { testSuiteFolder =>
                Files.move(testSuiteFolder.toPath, Path.of(rootFolderToUse, domainId.toString, testSuiteFolder.getName))
                counter += 1
              }
            }
            FileUtils.deleteQuietly(specificationFolder.toFile)
          }
        }
      } finally if (rs != null) rs.close()
    } finally if (select != null) select.close()
    LOG.info("Relocated " + counter + " test suite folders")
  }

}
