import {CustomProperty} from './custom-property.type';

export interface TestResultSearchCriteria {

  communityIds?: number[],
  domainIds?: number[],
  specIds?: number[],
  specGroupIds?: number[],
  actorIds?: number[],
  testSuiteIds?: number[],
  testCaseIds?: number[],
  organisationIds?: number[],
  systemIds?: number[],
  results?: string[],
  startTimeBeginStr?: string,
  startTimeEndStr?: string,
  endTimeBeginStr?: string,
  endTimeEndStr?: string,
  sessionId?: string,
  organisationProperties?: CustomProperty[],
  systemProperties?: CustomProperty[],
  activeSortColumn?: string,
  activeSortOrder?: string,
  completedSortColumn?: string,
  completedSortOrder?: string

}
