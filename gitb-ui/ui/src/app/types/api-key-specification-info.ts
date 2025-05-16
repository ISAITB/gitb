import { ApiKeyActorInfo } from "./api-key-actor-info";
import { ApiKeyTestSuiteInfo } from "./api-key-test-suite-info";

export interface ApiKeySpecificationInfo {

    id: number,
    name: string,
    actors: ApiKeyActorInfo[],
    testSuites: ApiKeyTestSuiteInfo[]

}
