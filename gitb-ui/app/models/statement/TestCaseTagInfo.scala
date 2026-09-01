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

package models.statement

import models.TestCaseTag
import utils.JsonUtil

import java.security.MessageDigest
import java.nio.charset.StandardCharsets

/**
 * Identifies a test case tag for filtering purposes, distinct on (name, foreground, background) as tags
 * are recorded separately per test case (not shared/reused rows).
 */
case class TestCaseTagInfo(key: String, name: String, foreground: String, background: String)

object TestCaseTagInfo {

  // Matches the defaults applied when rendering tags in reports (see ReportManager.parseTestCaseTags).
  val DEFAULT_FOREGROUND = "#777777"
  val DEFAULT_BACKGROUND = "#FFFFFF"

  def keyFor(name: String, foreground: String, background: String): String = {
    val digest = MessageDigest.getInstance("SHA-256").digest(s"$name|$foreground|$background".getBytes(StandardCharsets.UTF_8))
    digest.map("%02x".format(_)).mkString.substring(0, 16)
  }

  def normalise(tag: TestCaseTag): TestCaseTagInfo = {
    val foreground = tag.foreground.getOrElse(DEFAULT_FOREGROUND).toLowerCase
    val background = tag.background.getOrElse(DEFAULT_BACKGROUND).toLowerCase
    TestCaseTagInfo(keyFor(tag.name, foreground, background), tag.name, foreground, background)
  }

  /** The set of normalised tag keys carried by a test case's raw stored tags JSON. */
  def keysFor(tagsJson: Option[String]): Set[String] = {
    tagsJson match {
      case Some(json) if json.nonEmpty => JsonUtil.parseJsTags(json).map(tag => normalise(tag).key).toSet
      case _ => Set.empty
    }
  }

}
