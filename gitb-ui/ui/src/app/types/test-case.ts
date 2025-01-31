import {EntityWithId} from './entity-with-id';
import {SpecificationReferenceInfo} from './specification-reference-info';
import {TestCaseTag} from './test-case-tag';

export interface TestCase extends EntityWithId, SpecificationReferenceInfo {

  identifier: string
  sname: string,
  description?: string
  documentation?: string
  hasDocumentation?: boolean
  optional?: boolean
  disabled?: boolean
  tags?: string
  parsedTags?: TestCaseTag[]
  group?: number

}
