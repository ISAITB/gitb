import { KeyValue } from "src/app/types/key-value";

export interface PlaceholderInfo extends KeyValue {

    select?: () => string

}
