import { UserAccount } from "./user-account";

export interface ActualUserInfo {

    uid: string
    email: string
    firstName: string
    lastName: string
    accounts: UserAccount[]

}
