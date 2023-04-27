import { EntityWithId } from "../../types/entity-with-id";

export interface ItemMap<T extends EntityWithId> {
    [key: number]: { selected: boolean, item: T }
}
