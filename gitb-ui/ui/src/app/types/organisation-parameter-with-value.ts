import { OrganisationParameter } from "./organisation-parameter";

export interface OrganisationParameterWithValue extends OrganisationParameter {

    configured: boolean
    value?: string

}
