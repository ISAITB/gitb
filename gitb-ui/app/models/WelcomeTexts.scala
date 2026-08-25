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

package models

object WelcomeTexts {

  val TitleDefault = "Welcome to the Interoperability Test Bed"
  val LogInCardTitleDefault = "Log in"
  val LogInCardContentDefault = "Already have an account? Click to log in, run tests, review reports and manage your configuration."
  val ConfirmRoleCardTitleDefault = "Confirm new role"
  val ConfirmRoleCardContentDefault = "Assigned a new role by an administrator? Click to confirm the role assignment and start using it."
  val RegisterCardTitleDefault = "Register with community"
  val RegisterCardContentDefault = "Want to register with a public community? Click to provide your information and create your account."
  val DemoCardTitleDefault = "Try out our demos"
  val DemoCardContentDefault = "Interested in seeing how the Test Bed works? Click to explore curated demos with no need for an account."

  def defaultConfiguration(): WelcomeTexts = {
    WelcomeTexts(
      title = TitleDefault,
      logInCardTitle = LogInCardTitleDefault,
      logInCardContent = LogInCardContentDefault,
      confirmRoleCardTitle = ConfirmRoleCardTitleDefault,
      confirmRoleCardContent = ConfirmRoleCardContentDefault,
      registerCardTitle = RegisterCardTitleDefault,
      registerCardContent = RegisterCardContentDefault,
      demoCardTitle = DemoCardTitleDefault,
      demoCardContent = DemoCardContentDefault
    )
  }

}

/**
 * The Test Bed-wide (system administration) plain text values presented on the welcome page: the
 * banner title, and the title and content of each of the welcome page's option cards.
 */
case class WelcomeTexts(title: String,
                        logInCardTitle: String,
                        logInCardContent: String,
                        confirmRoleCardTitle: String,
                        confirmRoleCardContent: String,
                        registerCardTitle: String,
                        registerCardContent: String,
                        demoCardTitle: String,
                        demoCardContent: String)
