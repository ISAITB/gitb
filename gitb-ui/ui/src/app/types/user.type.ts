import { Organisation } from "./organisation.type";

export interface User {

    id?: number,
    name?: string,
    email?: string,
    role?: number,
    roleText?: string,
    onetime?: boolean,
    ssoStatus?: number,
    ssoStatusText?: string,
    organization?: Organisation,
    password?: string,

}
