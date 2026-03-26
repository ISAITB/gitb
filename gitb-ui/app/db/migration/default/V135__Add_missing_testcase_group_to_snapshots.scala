/*
 * Copyright (C) 2026 European Union
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

import java.sql.SQLException

class V135__Add_missing_testcase_group_to_snapshots extends BaseJavaMigration {

  private def LOG = LoggerFactory.getLogger(classOf[V135__Add_missing_testcase_group_to_snapshots])

  override def migrate(context: Context): Unit = {
    val statement = context.getConnection.createStatement()
    try {
      statement.execute("ALTER TABLE `conformancesnapshotresults` ADD COLUMN `test_case_group_id` BIGINT")
      LOG.info("Column 'test_case_group_id' added")
    } catch {
      case e: SQLException if e.getErrorCode == 1060 =>
        LOG.info("Column 'test_case_group_id' already present")
    } finally {
      statement.close()
    }
  }

}
