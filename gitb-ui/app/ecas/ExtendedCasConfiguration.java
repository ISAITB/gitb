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

import org.apereo.cas.client.validation.Cas20ServiceTicketValidator;
import org.apereo.cas.client.validation.TicketValidator;
import org.pac4j.cas.config.CasConfiguration;
import org.pac4j.cas.store.ProxyGrantingTicketStore;
import org.pac4j.core.context.WebContext;

/**
 * Extension class used to supply the ExtendedCas20ServiceTicketValidator to the configuration.
 */
public class ExtendedCasConfiguration extends CasConfiguration {

    @Override
    protected TicketValidator buildCas20TicketValidator(final WebContext context) {
        final Cas20ServiceTicketValidator cas20ServiceTicketValidator = new ExtendedCas20ServiceTicketValidator(computeFinalPrefixUrl(context));
        cas20ServiceTicketValidator.setEncoding(getEncoding());
        cas20ServiceTicketValidator.setRenew(isRenew());
        if (getProxyReceptor() != null) {
            cas20ServiceTicketValidator.setProxyCallbackUrl(getProxyReceptor().computeFinalCallbackUrl(context));
            cas20ServiceTicketValidator.setProxyGrantingTicketStorage(new ProxyGrantingTicketStore(getProxyReceptor().getStore()));
        }
        addPrivateKey(cas20ServiceTicketValidator);
        return cas20ServiceTicketValidator;
    }

}
