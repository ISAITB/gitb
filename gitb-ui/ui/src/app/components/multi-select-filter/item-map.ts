import { MultiSelectItem } from "./multi-select-item";

export interface ItemMap {
    [key: number]: { selected: boolean, item: MultiSelectItem }
}
