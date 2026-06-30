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

import exceptions.UnacceptableUriException
import org.apache.commons.lang3.RandomStringUtils
import org.apache.pekko.stream.Materializer
import org.apache.pekko.stream.scaladsl.FileIO
import org.slf4j.LoggerFactory
import play.api.libs.ws.WSClient
import utils.{RepositoryUtils, UriAuthorizer}

import java.io.File
import java.net.URI
import java.nio.file.Paths
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

/**
 * Fetches a remote archive to a local temp file, with SSRF protection applied on each hop.
 */
@Singleton
class RemoteArchiveFetcher @Inject()(ws: WSClient, repositoryUtils: RepositoryUtils)(implicit ec: ExecutionContext, mat: Materializer) {

  private val logger = LoggerFactory.getLogger(classOf[RemoteArchiveFetcher])
  private val MaxRedirects = 5

  /**
   * Validate the URI, download the archive to a temp file, and return the file.
   *
   * HTTP redirects are followed manually (up to [[MaxRedirects]] hops), with the SSRF check
   * re-applied on every redirect target so that redirect-based bypass is not possible.
   *
   * @param uriStr The URI to fetch the archive from.
   * @return The downloaded temp file.
   * @throws UnacceptableUriException if the URI is blocked by the SSRF guard or is invalid.
   */
  def fetchToTempFile(uriStr: String): Future[File] = {
    val parsedUri = try {
      URI.create(uriStr)
    } catch {
      case _: IllegalArgumentException =>
        return Future.failed(UnacceptableUriException("The provided URI is not valid."))
    }
    try {
      UriAuthorizer.assertAllowed(parsedUri)
    } catch {
      case e: UnacceptableUriException => return Future.failed(e)
    }
    fetchWithRedirects(uriStr, remainingHops = MaxRedirects)
  }

  private def fetchWithRedirects(uriStr: String, remainingHops: Int): Future[File] = {
    logger.debug("Fetching remote test suite archive from: {}", uriStr)
    ws.url(uriStr)
      .withFollowRedirects(false)
      .withMethod("GET")
      .stream()
      .flatMap { response =>
        val status = response.status
        if (status >= 300 && status < 400) {
          // Redirect: validate and follow the Location header.
          if (remainingHops <= 0) {
            Future.failed(UnacceptableUriException("Too many redirects while fetching the test suite archive."))
          } else {
            response.headers.get("Location").flatMap(_.headOption) match {
              case Some(location) =>
                // Resolve relative Location against the current URI.
                val redirectUri = try {
                  URI.create(uriStr).resolve(location)
                } catch {
                  case _: IllegalArgumentException =>
                    return Future.failed(UnacceptableUriException("Redirect location is not a valid URI."))
                }
                try {
                  UriAuthorizer.assertAllowed(redirectUri)
                } catch {
                  case e: UnacceptableUriException => return Future.failed(e)
                }
                fetchWithRedirects(redirectUri.toString, remainingHops - 1)
              case None =>
                Future.failed(UnacceptableUriException("Received a redirect response without a Location header."))
            }
          }
        } else if (status >= 200 && status < 300) {
          // Success: stream the body into a temp file.
          val testSuiteFileName = "ts_" + RandomStringUtils.secure.next(10, false, true) + ".zip"
          val tempPath = Paths.get(
            repositoryUtils.getTempFolder().getAbsolutePath,
            RandomStringUtils.secure.next(10, false, true),
            testSuiteFileName
          )
          tempPath.toFile.getParentFile.mkdirs()
          response.bodyAsSource.runWith(FileIO.toPath(tempPath)).map { result =>
            if (result.wasSuccessful) {
              logger.debug("Downloaded remote test suite archive to: {}", tempPath)
              tempPath.toFile
            } else {
              throw result.getError
            }
          }
        } else {
          Future.failed(UnacceptableUriException(s"Failed to read the test suite archive from the provided URI (HTTP $status)."))
        }
      }
  }

}
