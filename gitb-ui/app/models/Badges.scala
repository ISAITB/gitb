package models

object Badges {

  def noBadges(): Badges = {
    Badges(hasSuccess = false, hasFailure = false, hasOther = false, None, None, None)
  }

}

case class Badges(
                   hasSuccess: Boolean, hasFailure: Boolean, hasOther: Boolean,
                   success: Option[NamedFile], failure: Option[NamedFile], other: Option[NamedFile]
)
