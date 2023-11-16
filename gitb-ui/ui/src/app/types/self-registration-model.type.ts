import { SelfRegistrationOption } from "./self-registration-option.type";
import { SelfRegistrationTemplate } from "./self-registration-template.type";

export interface SelfRegistrationModel {

    selfRegToken?: string,
    selfRegOption?: SelfRegistrationOption,
    template?: SelfRegistrationTemplate,
    orgFullName?: string,
    orgShortName?: string,
    adminName?: string,
    adminEmail?: string,
    adminPassword?: string,

}
