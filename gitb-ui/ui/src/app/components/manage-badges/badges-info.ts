import { BadgeInfo } from "./badge-info";

export interface BadgesInfo {

    specificationId?: number
    actorId?: number
    success: BadgeInfo
    failure: BadgeInfo
    other: BadgeInfo

    enabled: boolean
    failureBadgeActive: boolean
    initiallyEnabled: boolean
}
