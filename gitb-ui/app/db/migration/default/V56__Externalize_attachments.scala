package db.migration.default

import org.flywaydb.core.api.migration.{BaseJavaMigration, Context}
import org.slf4j.LoggerFactory

import java.nio.file.{Files, Path}
import java.util.Base64

class V56__Externalize_attachments extends BaseJavaMigration {

  private def LOG = LoggerFactory.getLogger(classOf[V56__Externalize_attachments])
  private def DEFAULT_ROOT_FOLDER = "/gitb-repository"

  private def contentTypeFromDataURL(value: String): String = {
    value.substring(5, value.indexOf(";base64,"))
  }

  private def base64FromDataURL(dataURL: String): String = {
    dataURL.substring(dataURL.indexOf(",") + 1)
  }

  private def createAttachment(dataURL: String, path: String, rootFolder: String): (String, String) = {
    val filePath = Path.of(rootFolder, path)
    filePath.getParent.toFile.mkdirs()
    Files.write(filePath, Base64.getDecoder.decode(base64FromDataURL(dataURL)))
    (path, contentTypeFromDataURL(dataURL))
  }

  override def migrate(context: Context): Unit = {
    val repositoryFolder: String = sys.env.get("TESTBED_REPOSITORY_PATH").orElse(Some(DEFAULT_ROOT_FOLDER)).get
    val rootFolderToUse = Path.of(repositoryFolder, "files").toString

    // Domain parameters.
    {
      var counter = 0
      val select = context.getConnection.createStatement
      try {
        val rs = select.executeQuery("SELECT `id`, `value`, `domain` from `domainparameters` WHERE `value` IS NOT NULL AND `kind` = 'BINARY'")
        try {
          while (rs.next) {
            val id = rs.getLong(1)
            val value = rs.getString(2)
            val domain = rs.getLong(3)
            val attachmentData = createAttachment(value, "/dp/"+domain+"/"+id, rootFolderToUse)
            val update = context.getConnection.prepareStatement("UPDATE `domainparameters` SET `content_type` = ?, `value` = ? WHERE `id` = ?")
            try {
              update.setString(1, attachmentData._2)
              update.setString(2, attachmentData._1)
              update.setLong(3, id)
              update.executeUpdate()
            } finally if (update != null) update.close()
            counter += 1
          }
        } finally if (rs != null) rs.close()
      } finally if (select != null) select.close()
      LOG.info("Updated "+counter+" domain parameter(s)")
    }
    // Organisation properties.
    {
      var counter = 0
      val select = context.getConnection.createStatement
      try {
        val rs = select.executeQuery("SELECT v.`organisation`, v.`parameter`, v.`value` FROM `organisationparametervalues` v JOIN `organisationparameters` p on v.`parameter` = p.`id` WHERE p.`kind` = 'BINARY'")
        try {
          while (rs.next) {
            val organisationId = rs.getLong(1)
            val parameterId = rs.getLong(2)
            val value = rs.getString(3)
            val attachmentData = createAttachment(value, "/op/"+parameterId+"/"+organisationId+"_"+parameterId, rootFolderToUse)
            val update = context.getConnection.prepareStatement("UPDATE `organisationparametervalues` SET `content_type` = ?, `value` = ? WHERE `organisation` = ? AND `parameter` = ?")
            try {
              update.setString(1, attachmentData._2)
              update.setString(2, attachmentData._1)
              update.setLong(3, organisationId)
              update.setLong(4, parameterId)
              update.executeUpdate()
            } finally if (update != null) update.close()
            counter += 1
          }
        } finally if (rs != null) rs.close()
      } finally if (select != null) select.close()
      LOG.info("Updated "+counter+" organisation property value(s)")
    }
    // System properties
    {
      var counter = 0
      val select = context.getConnection.createStatement
      try {
        val rs = select.executeQuery("SELECT v.`system`, v.`parameter`, v.`value` FROM `systemparametervalues` v JOIN `systemparameters` p on v.`parameter` = p.`id` WHERE p.`kind` = 'BINARY'")
        try {
          while (rs.next) {
            val systemId = rs.getLong(1)
            val parameterId = rs.getLong(2)
            val value = rs.getString(3)
            val attachmentData = createAttachment(value, "/sp/"+parameterId+"/"+systemId+"_"+parameterId, rootFolderToUse)
            val update = context.getConnection.prepareStatement("UPDATE `systemparametervalues` SET `content_type` = ?, `value` = ? WHERE `system` = ? AND `parameter` = ?")
            try {
              update.setString(1, attachmentData._2)
              update.setString(2, attachmentData._1)
              update.setLong(3, systemId)
              update.setLong(4, parameterId)
              update.executeUpdate()
            } finally if (update != null) update.close()
            counter += 1
          }
        } finally if (rs != null) rs.close()
      } finally if (select != null) select.close()
      LOG.info("Updated "+counter+" system property value(s)")
    }
    // Statement properties
    {
      var counter = 0
      val select = context.getConnection.createStatement
      try {
        val rs = select.executeQuery("SELECT c.`system`, c.`parameter`, c.`value` FROM `configurations` c JOIN `parameters` p on c.`parameter` = p.`id` WHERE p.`kind` = 'BINARY'")
        try {
          while (rs.next) {
            val systemId = rs.getLong(1)
            val parameterId = rs.getLong(2)
            val value = rs.getString(3)
            val attachmentData = createAttachment(value, "/ep/"+parameterId+"/"+systemId+"_"+parameterId, rootFolderToUse)
            val update = context.getConnection.prepareStatement("UPDATE `configurations` SET `content_type` = ?, `value` = ? WHERE `system` = ? AND `parameter` = ?")
            try {
              update.setString(1, attachmentData._2)
              update.setString(2, attachmentData._1)
              update.setLong(3, systemId)
              update.setLong(4, parameterId)
              update.executeUpdate()
            } finally if (update != null) update.close()
            counter += 1
          }
        } finally if (rs != null) rs.close()
      } finally if (select != null) select.close()
      LOG.info("Updated "+counter+" conformance statement parameter(s)")
    }
  }

}
