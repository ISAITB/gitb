import { ApiKeySpecificationInfo } from "./api-key-specification-info";
import { ApiKeySystemInfo } from "./api-key-system-info";

export interface ApiKeyInfo {

    organisation?: string,
    systems: ApiKeySystemInfo[],
    specifications: ApiKeySpecificationInfo[]

}
