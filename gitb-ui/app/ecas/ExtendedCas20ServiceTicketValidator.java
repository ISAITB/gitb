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

package ecas;

import config.Configurations;
import models.Constants;
import org.apereo.cas.client.util.XmlUtils;
import org.apereo.cas.client.validation.Assertion;
import org.apereo.cas.client.validation.Cas20ServiceTicketValidator;
import org.apereo.cas.client.validation.TicketValidationException;

import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

/**
 * Custom ticket validator to ensure that the features of the ECAS client are reproduced.
 * These are notably:
 * - The request of the user details.
 * - The ticket validation URL to allow also self-registered users.
 */
public class ExtendedCas20ServiceTicketValidator extends Cas20ServiceTicketValidator {

    private final EnumSet<AuthenticationLevel> acceptableAuthenticationLevels;

    /**
     * Constructs an instance of the CAS 2.0 Service Ticket Validator with the supplied
     * CAS server url prefix.
     *
     * @param casServerUrlPrefix the CAS Server URL prefix.
     */
    public ExtendedCas20ServiceTicketValidator(String casServerUrlPrefix) {
        super(casServerUrlPrefix);
        setCustomParameters(Collections.singletonMap(Configurations.AUTHENTICATION_SSO_CUSTOM_PARAMETERS__USER_DETAILS(), "true"));
        if (Configurations.AUTHENTICATION_SSO_AUTHENTICATION_LEVEL().isDefined()) {
            this.acceptableAuthenticationLevels = switch (Configurations.AUTHENTICATION_SSO_AUTHENTICATION_LEVEL().get()) {
                case AuthenticationLevel.HIGH -> EnumSet.of(AuthenticationLevel.HIGH);
                case AuthenticationLevel.MEDIUM -> EnumSet.of(AuthenticationLevel.MEDIUM, AuthenticationLevel.HIGH);
                default -> EnumSet.of(AuthenticationLevel.BASIC, AuthenticationLevel.MEDIUM, AuthenticationLevel.HIGH);
            };
        } else {
            this.acceptableAuthenticationLevels = null;
        }
    }

    @Override
    protected String getUrlSuffix() {
        return Configurations.AUTHENTICATION_SSO_TICKET_VALIDATION_URL_SUFFIX();
    }

    @Override
    protected void customParseResponse(final String response, final Assertion assertion) throws TicketValidationException {
        if (this.acceptableAuthenticationLevels != null) {
            if (assertion == null || assertion.getPrincipal() == null || assertion.getPrincipal().getAttributes() == null) {
                throw new TicketValidationException("Expected authentication level [%s] but was unable to retrieve it from the response".formatted(Configurations.AUTHENTICATION_SSO_AUTHENTICATION_LEVEL().get()));
            } else {
                AuthenticationLevel returnedLevel = AuthenticationLevel.fromName(String.valueOf(assertion.getPrincipal().getAttributes().get(Constants.UserAttributeAuthenticationLevel())));
                if (!acceptableAuthenticationLevels.contains(returnedLevel)) {
                    throw new TicketValidationException("Expected authentication level [%s] but received [%s]".formatted(Configurations.AUTHENTICATION_SSO_AUTHENTICATION_LEVEL().get(), returnedLevel));
                }
            }
        }
    }

    @Override
    protected Map<String, Object> extractCustomAttributes(final String xml) {
        Map<String, Object> attributes = super.extractCustomAttributes(xml);
        if (attributes == null) {
            attributes = new HashMap<>(3);
        }
        attributes.put(Constants.UserAttributeEmail(), XmlUtils.getTextForElement(xml, Configurations.AUTHENTICATION_SSO_USER_ATTRIBUTES__EMAIL()));
        attributes.put(Constants.UserAttributeFirstName(), XmlUtils.getTextForElement(xml, Configurations.AUTHENTICATION_SSO_USER_ATTRIBUTES__FIRST_NAME()));
        attributes.put(Constants.UserAttributeLastName(), XmlUtils.getTextForElement(xml, Configurations.AUTHENTICATION_SSO_USER_ATTRIBUTES__LAST_NAME()));
        attributes.put(Constants.UserAttributeAuthenticationLevel(), XmlUtils.getTextForElement(xml, Configurations.AUTHENTICATION_SSO_USER_ATTRIBUTES__AUTHENTICATION_LEVEL()));
        return attributes;
    }

}
