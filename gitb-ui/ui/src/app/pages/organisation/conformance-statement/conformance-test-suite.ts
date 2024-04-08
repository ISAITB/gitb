import { SpecificationReferenceInfo } from "src/app/types/specification-reference-info";
import { ConformanceTestCase } from "./conformance-test-case";

export interface ConformanceTestSuite extends SpecificationReferenceInfo {

    id: number
    sname: string
    description?: string
    result: string
    hasDocumentation: boolean
    testCases: ConformanceTestCase[]

    expanded?: boolean
    hasOptionalTestCases?: boolean
    hasDisabledTestCases?: boolean

}
