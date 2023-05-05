import { EntityWithId } from "../../types/entity-with-id";

export interface FilterValues<T extends EntityWithId> {

    // Active values to take into account when searching.
    active: T[],
    // Other values matching the active ones' text value that should not be considered when searching.
    other: T[]

}