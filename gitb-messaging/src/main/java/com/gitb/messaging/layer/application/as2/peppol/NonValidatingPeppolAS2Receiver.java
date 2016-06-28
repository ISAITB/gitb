package com.gitb.messaging.layer.application.as2.peppol;

import com.gitb.core.Configuration;
import com.gitb.messaging.model.SessionContext;
import com.gitb.messaging.model.TransactionContext;
import com.gitb.utils.XMLUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Node;

import java.util.List;

/**
 * Created by simatosc on 27/06/2016.
 */
public class NonValidatingPeppolAS2Receiver extends PeppolAS2Receiver {

    private Logger logger = LoggerFactory.getLogger(PeppolAS2Receiver.class);

    public NonValidatingPeppolAS2Receiver(SessionContext sessionContext, TransactionContext transactionContext) {
        super(sessionContext, transactionContext);
    }

    @Override
    boolean checkSBDH(Node sbdh, List<Configuration> configurations){
        Node businessScope = XMLUtils.getChildNodeByName(sbdh, "BusinessScope");
        if (businessScope == null) {
            saveError("unexpected-processor-error", "No 'BusinessScope' element " +
                    "found in received standard business document header");
            return false;
        }
        return true;
    }

}
