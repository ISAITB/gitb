package models.statement

import models.Constants

import java.util.regex.Pattern
import java.util.stream.Collectors
import scala.collection.mutable.ListBuffer

object BadgePlaceholderInfo {

  // Badge placeholders can be $BADGE or e.g. $BADGE{100} (with width)
  private val REGEXP = Pattern.compile("(\\"+Constants.PlaceholderBadge+"(?:\\{\\d+\\})?)")
  private val PLACEHOLDER_BASE_LENGTH = Constants.PlaceholderBadge.length

  def findBadgeDefinitions(message: String): List[BadgePlaceholderInfo] = {
    val placeholders = new ListBuffer[BadgePlaceholderInfo]
    val matches = REGEXP.matcher(message).results().collect(Collectors.toList())
    matches.forEach { placeholderMatch =>
      val placeholder = placeholderMatch.group(1)
      val width = if (placeholder.length > PLACEHOLDER_BASE_LENGTH) {
        // We have a width defined.
        Some(Integer.parseInt(placeholder.substring(PLACEHOLDER_BASE_LENGTH+1, placeholder.length-1)))
      } else {
        None
      }
      placeholders += BadgePlaceholderInfo(placeholder, None, width)
    }
    placeholders.toList
  }

}

case class BadgePlaceholderInfo(placeholder: String, index: Option[Short], width: Option[Int])