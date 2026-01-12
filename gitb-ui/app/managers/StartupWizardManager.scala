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

package managers

import managers.`export`.ImportCompleteManager
import models.Constants
import models.health.SoftwareVersionCheckSettings
import org.apache.commons.io.FileUtils
import play.api.db.slick.DatabaseConfigProvider
import utils.{JsonUtil, RepositoryUtils}

import java.nio.file.{Files, Path}
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Using

@Singleton
class StartupWizardManager @Inject() (systemConfigurationManager: SystemConfigurationManager,
                                      importCompleteManager: ImportCompleteManager,
                                      repositoryUtils: RepositoryUtils,
                                      dbConfigProvider: DatabaseConfigProvider)
                                     (implicit ec: ExecutionContext) extends BaseManager(dbConfigProvider) {

  import dbConfig.profile.api._

  def completeStartupWizard(samples: Boolean, updates: Boolean, api: Boolean): Future[Unit] = {
    val action =       for {
      // Disable the startup wizard
      _ <- systemConfigurationManager.updateSystemParameterInternal(Constants.StartupWizard, Some("false"), applySetting = true)
      // Configure the REST API
      _ <- systemConfigurationManager.updateSystemParameterInternal(Constants.RestApiEnabled, Some(api.toString), applySetting = true)
      // Configure software update checks
      _ <- {
        val config = SoftwareVersionCheckSettings.fromEnvironment().copy(enabled = updates)
        systemConfigurationManager.updateSystemParameterInternal(Constants.SoftwareVersionCheck, Some(JsonUtil.jsSoftwareVersionCheckSettings(config).toString()), applySetting = true)
      }
    } yield ()
    DB.run(action.transactionally).flatMap { _ =>
      // Import samples (if needed)
      if (samples) {
        val samplesArchive = Path.of(repositoryUtils.getTempFolder().getAbsolutePath, "samples_%s.zip".formatted(System.currentTimeMillis()))
        Using.resource(Thread.currentThread().getContextClassLoader.getResourceAsStream("other/samples.zip")) { stream =>
          Files.copy(stream, samplesArchive)
        }
        importCompleteManager.importSandboxData(samplesArchive.toFile, "samples", checkArchiveHash = false).map { result =>
          if (!result.processingComplete) {
            throw new IllegalStateException("Unexpected error occurred while importing the samples")
          }
        }.andThen { _ =>
          FileUtils.deleteQuietly(samplesArchive.toFile)
        }
      } else {
        Future.successful(())
      }
    }
  }

}
