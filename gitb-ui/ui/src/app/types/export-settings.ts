/*
 * Copyright (C) 2025 European Union
 *
 * Licensed under the EUPL, Version 1.2 or - as soon they will be approved by the European Commission - subsequent
 * versions of the EUPL (the "Licence"); You may not use this work except in compliance with the Licence.
 *
 * You may obtain a copy of the Licence at:
 *
 * https://interoperable-europe.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the Licence is distributed on an
 * "AS IS" basis, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the Licence for
 * the specific language governing permissions and limitations under the Licence.
 */

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
