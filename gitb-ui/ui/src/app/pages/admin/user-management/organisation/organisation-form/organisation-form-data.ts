import { Organisation } from "src/app/types/organisation.type";

export interface OrganisationFormData extends Organisation {

    otherOrganisations?: number
    copyOrganisationParameters: boolean
    copySystemParameters: boolean
    copyStatementParameters: boolean

}
