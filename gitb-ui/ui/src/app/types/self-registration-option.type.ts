import { CustomProperty } from "./custom-property.type";
import { SelfRegistrationTemplate } from "./self-registration-template.type";
import { TypedLabelConfig } from "./typed-label-config.type";

export interface SelfRegistrationOption {

    communityId?: number,
    selfRegType: number,
    labels: TypedLabelConfig[],
    forceTemplateSelection: boolean,
    forceRequiredProperties: boolean,
    templates?: SelfRegistrationTemplate[],
    organisationProperties?: CustomProperty[],
    selfRegTokenHelpText?: string

}
