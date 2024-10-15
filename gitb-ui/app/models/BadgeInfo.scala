package models

object BadgeInfo {

  def noBadges(): BadgeInfo = {
    BadgeInfo(Badges.noBadges(), Badges.noBadges())
  }

}

case class BadgeInfo(forWeb: Badges, forReport: Badges)
