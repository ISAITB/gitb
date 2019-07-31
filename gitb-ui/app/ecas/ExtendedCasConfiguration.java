package ecas;

import org.jasig.cas.client.validation.Cas20ServiceTicketValidator;
import org.jasig.cas.client.validation.TicketValidator;
import org.pac4j.cas.config.CasConfiguration;
import org.pac4j.cas.store.ProxyGrantingTicketStore;
import org.pac4j.core.context.WebContext;

public class ExtendedCasConfiguration extends CasConfiguration {

    @Override
    protected TicketValidator buildCas20TicketValidator(final WebContext context) {
        final Cas20ServiceTicketValidator cas20ServiceTicketValidator = new ExtendedCas20ServiceTicketValidator(computeFinalPrefixUrl(context));
        cas20ServiceTicketValidator.setEncoding(getEncoding());
        if (getProxyReceptor() != null) {
            cas20ServiceTicketValidator.setProxyCallbackUrl(getProxyReceptor().computeFinalCallbackUrl(context));
            cas20ServiceTicketValidator.setProxyGrantingTicketStorage(new ProxyGrantingTicketStore(getProxyReceptor().getStore()));
        }
        return cas20ServiceTicketValidator;
    }

}
