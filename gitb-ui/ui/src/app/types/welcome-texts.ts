/*
 * Copyright (C) 2026 European Union
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

/**
 * The plain text values presented on the welcome page, persisted as a single JSON system
 * configuration value (see Constants.SYSTEM_CONFIG.WELCOME_TEXTS). Property names match the Scala
 * models.WelcomeTexts case class.
 */
export interface WelcomeTexts {
  title?: string
  logInCardTitle?: string
  logInCardContent?: string
  confirmRoleCardTitle?: string
  confirmRoleCardContent?: string
  registerCardTitle?: string
  registerCardContent?: string
  demoCardTitle?: string
  demoCardContent?: string
}
