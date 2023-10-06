package models

case class Badges(
  hasSuccess: Boolean, hasFailure: Boolean, hasOther: Boolean,
  success: Option[BadgeFile], failure: Option[BadgeFile], other: Option[BadgeFile]
)
