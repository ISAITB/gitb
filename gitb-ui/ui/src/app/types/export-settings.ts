export interface ExportSettings {

  landingPages: boolean
  errorTemplates: boolean
  triggers: boolean
  resources: boolean
  legalNotices: boolean
  certificateSettings: boolean
  customLabels: boolean
  customProperties: boolean
  organisations: boolean
  organisationPropertyValues: boolean
  systems: boolean
  systemPropertyValues: boolean
  statements: boolean
  statementConfigurations: boolean
  domain: boolean
  domainParameters: boolean
  specifications: boolean
  actors: boolean
  endpoints: boolean
  testSuites: boolean
  communityAdministrators: boolean
  organisationUsers: boolean
  systemResources: boolean
  themes: boolean
  defaultLandingPages: boolean
  defaultLegalNotices: boolean
  defaultErrorTemplates: boolean
  systemAdministrators: boolean
  systemConfigurations: boolean
  communitiesToDelete?: string[]
  domainsToDelete?: string[]
  encryptionKey?: string

}
