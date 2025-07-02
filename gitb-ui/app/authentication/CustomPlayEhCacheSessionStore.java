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

package authentication;

import com.google.inject.Provider;
import org.pac4j.play.store.PlayCacheSessionStore;
import play.cache.SyncCacheApi;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Map;

/**
 * Extension of the PlayEhCacheSessionStore from Pac4J that allows also the setting of a maximum
 * time for sessions apart from the idle timeout.
 */
@Singleton
public class CustomPlayEhCacheSessionStore extends PlayCacheSessionStore {

    @Inject
    public CustomPlayEhCacheSessionStore(SyncCacheApi cache) {
        this.store = new CustomPlayEhCacheStore<>(cache);
        setDefaultTimeout();
    }

    public CustomPlayEhCacheSessionStore(Provider<SyncCacheApi> cacheProvider) {
        this.store = new CustomPlayEhCacheStore<>(cacheProvider);
        setDefaultTimeout();
    }

    public void setMaxTimeout(int maxTimeout) {
        ((CustomPlayEhCacheStore<String, Map<String, Object>>)this.store).setMaxTimeout(maxTimeout);
    }

}
