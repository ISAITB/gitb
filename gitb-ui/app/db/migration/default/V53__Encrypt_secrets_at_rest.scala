/*
 * Copyright (C) 2025 European Union
 *
 * Licensed under the EUPL, Version 1.2 or - as soon they will be approved by the European Commission - subsequent
 * versions of the EUPL (the "Licence"); You may not use this work except in compliance with the Licence.
 *
 * You may obtain a copy of the Licence at:
 *
 * https://interoperable-europe.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the Licence is distributed on an
 * "AS IS" basis, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the Licence for
 * the specific language governing permissions and limitations under the Licence.
 */

package db.migration.default

import org.flywaydb.core.api.migration.{BaseJavaMigration, Context}
import org.slf4j.LoggerFactory
import utils.MimeUtil

class V53__Encrypt_secrets_at_rest extends BaseJavaMigration {

  private def LOG = LoggerFactory.getLogger(classOf[V53__Encrypt_secrets_at_rest])

  override def migrate(context: Context): Unit = {
    // Domain parameters.
    {
      var counter = 0
      val select = context.getConnection.createStatement
      try {
        val rs = select.executeQuery("SELECT `id`, `value` from `domainparameters` WHERE `value` IS NOT NULL AND `kind` = 'HIDDEN'")
        try {
          while (rs.next) {
            val id = rs.getLong(1)
            val value = rs.getString(2)
            val update = context.getConnection.prepareStatement("UPDATE `domainparameters` SET `value` = ? WHERE `id` = ?")
            try {
              update.setString(1, MimeUtil.encryptString(value))
              update.setLong(2, id)
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
        val rs = select.executeQuery("SELECT v.`organisation`, v.`parameter`, v.`value` " + "FROM `organisationparametervalues` v " + "JOIN `organisationparameters` p on v.`parameter` = p.`id` " + "WHERE p.`kind` = 'SECRET'")
        try {
          while (rs.next) {
            val organisationId = rs.getLong(1)
            val parameterId = rs.getLong(2)
            val value = rs.getString(3)
            val update = context.getConnection.prepareStatement("UPDATE `organisationparametervalues` SET `value` = ? WHERE `organisation` = ? AND `parameter` = ?")
            try {
              update.setString(1, MimeUtil.encryptString(value))
              update.setLong(2, organisationId)
              update.setLong(3, parameterId)
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
        val rs = select.executeQuery("SELECT v.`system`, v.`parameter`, v.`value` " + "FROM `systemparametervalues` v " + "JOIN `systemparameters` p on v.`parameter` = p.`id` " + "WHERE p.`kind` = 'SECRET'")
        try {
          while (rs.next) {
            val systemId = rs.getLong(1)
            val parameterId = rs.getLong(2)
            val value = rs.getString(3)
            val update = context.getConnection.prepareStatement("UPDATE `systemparametervalues` SET `value` = ? WHERE `system` = ? AND `parameter` = ?")
            try {
              update.setString(1, MimeUtil.encryptString(value))
              update.setLong(2, systemId)
              update.setLong(3, parameterId)
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
        val rs = select.executeQuery("SELECT c.`system`, c.`parameter`, c.`value` " + "FROM `configurations` c " + "JOIN `parameters` p on c.`parameter` = p.`id` " + "WHERE p.`kind` = 'SECRET'")
        try {
          while (rs.next) {
            val systemId = rs.getLong(1)
            val parameterId = rs.getLong(2)
            val value = rs.getString(3)
            val update = context.getConnection.prepareStatement("UPDATE `configurations` SET `value` = ? WHERE `system` = ? AND `parameter` = ?")
            try {
              update.setString(1, MimeUtil.encryptString(value))
              update.setLong(2, systemId)
              update.setLong(3, parameterId)
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
