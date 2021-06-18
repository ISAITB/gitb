import { TestCase } from "./test-case";
import { TestSuite } from "./test-suite";

export interface TestSuiteWithTestCases extends TestSuite {

    testCases: TestCase[]

}
