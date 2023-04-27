import { EntityWithId } from "../../types/entity-with-id";
import { FilterValues } from "./filter-values";

export interface FilterUpdate<T extends EntityWithId> {

    values: FilterValues<T>
    applyFilters: boolean

}