import { EntityWithId } from "./entity-with-id";

export interface System extends EntityWithId {

    sname: string,
    fname: string,
    version?: string,
    description?: string,
    hasTests?: boolean,
    apiKey: string,
    owner: number

}
