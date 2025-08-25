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

package managers.export

class ExportSettings {

  var communityAdministrators: Boolean = false
  var landingPages: Boolean = false
  var legalNotices: Boolean = false
  var errorTemplates: Boolean = false
  var triggers: Boolean = false
  var resources: Boolean = false
  var certificateSettings: Boolean = false
  var customLabels: Boolean = false
  var customProperties: Boolean = false
  var organisations: Boolean = false
  var organisationUsers: Boolean = false
  var organisationPropertyValues: Boolean = false
  var systems: Boolean = false
  var systemPropertyValues: Boolean = false
  var statements: Boolean = false
  var statementConfigurations: Boolean = false
  var domain: Boolean = false
  var domainParameters: Boolean = false
  var testServices: Boolean = false
  var specifications: Boolean = false
  var actors: Boolean = false
  var endpoints: Boolean = false
  var testSuites: Boolean = true
  var themes: Boolean = false
  var systemResources: Boolean = false
  var systemAdministrators: Boolean = false
  var defaultLandingPages: Boolean = false
  var defaultLegalNotices: Boolean = false
  var defaultErrorTemplates: Boolean = false
  var systemConfigurations: Boolean = false
  var encryptionKey: Option[String] = None
  var communitiesToDelete: Option[List[String]] = None
  var domainsToDelete: Option[List[String]] = None

  def hasSystemSettings(): Boolean = {
    systemResources || themes || systemAdministrators || defaultLandingPages || defaultLegalNotices || defaultErrorTemplates || systemConfigurations
  }

  def withoutSystemSettings(): ExportSettings = {
    if (this.themes) {
      val copy = new ExportSettings
      copy.communityAdministrators = this.communityAdministrators
      copy.landingPages = this.landingPages
      copy.legalNotices = this.legalNotices
      copy.errorTemplates = this.errorTemplates
      copy.triggers = this.triggers
      copy.resources = this.resources
      copy.certificateSettings = this.certificateSettings
      copy.customLabels = this.customLabels
      copy.customProperties = this.customProperties
      copy.organisations = this.organisations
      copy.organisationUsers = this.organisationUsers
      copy.organisationPropertyValues = this.organisationPropertyValues
      copy.systems = this.systems
      copy.systemPropertyValues = this.systemPropertyValues
      copy.statements = this.statements
      copy.statementConfigurations = this.statementConfigurations
      copy.domain = this.domain
      copy.domainParameters = this.domainParameters
      copy.testServices = this.testServices
      copy.specifications = this.specifications
      copy.actors = this.actors
      copy.endpoints = this.endpoints
      copy.testSuites = this.testSuites
      copy.systemResources = false
      copy.themes = false
      copy.systemAdministrators = false
      copy.defaultLandingPages = false
      copy.defaultLegalNotices = false
      copy.defaultErrorTemplates = false
      copy.systemConfigurations = false
      copy.encryptionKey = this.encryptionKey
      copy.communitiesToDelete = this.communitiesToDelete
      copy.domainsToDelete = this.domainsToDelete
      copy
    } else {
      this
    }
  }
}
