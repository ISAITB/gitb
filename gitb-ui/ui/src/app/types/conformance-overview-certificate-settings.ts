import { ConformanceOverviewMessage } from "../pages/admin/user-management/community-reports/conformance-overview-message";
import { CertificateSettings } from "./certificate-settings";

export interface ConformanceOverviewCertificateSettings extends CertificateSettings {

    includeTestCaseDetails: boolean
    enableAllLevel: boolean
    enableDomainLevel: boolean
    enableGroupLevel: boolean
    enableSpecificationLevel: boolean
    messages?: ConformanceOverviewMessage[]
}
