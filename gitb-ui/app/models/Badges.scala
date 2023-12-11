package models

case class Badges(
                   hasSuccess: Boolean, hasFailure: Boolean, hasOther: Boolean,
                   success: Option[NamedFile], failure: Option[NamedFile], other: Option[NamedFile]
)
