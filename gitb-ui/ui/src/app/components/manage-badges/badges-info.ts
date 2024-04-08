import { BadgeInfo } from "./badge-info";

export interface BadgesInfo {

    specificationId?: number
    actorId?: number
    success: BadgeInfo
    failure: BadgeInfo
    other: BadgeInfo
    successForReport: BadgeInfo
    failureForReport: BadgeInfo
    otherForReport: BadgeInfo

    enabled: boolean
    failureBadgeActive: boolean
    successBadgeForReportActive: boolean
    otherBadgeForReportActive: boolean
    failureBadgeForReportActive: boolean
    initiallyEnabled: boolean
}
