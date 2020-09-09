package modules;

import org.pac4j.core.authorization.authorizer.Authorizer;
import org.pac4j.core.context.WebContext;
import org.pac4j.core.profile.CommonProfile;

import java.util.List;

public class BasicAuthorizer<U extends CommonProfile> implements Authorizer<U> {

    @Override
    public boolean isAuthorized(WebContext context, List<U> profiles) {
        return true;
    }

}
