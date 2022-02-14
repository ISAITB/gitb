import { ApiKeyActorInfo } from "./api-key-actor-info";
import { ApiKeyTestSuiteInfo } from "./api-key-test-suite-info";

export interface ApiKeySpecificationInfo {

    name: string,
    actors: ApiKeyActorInfo[],
    testSuites: ApiKeyTestSuiteInfo[]

}
