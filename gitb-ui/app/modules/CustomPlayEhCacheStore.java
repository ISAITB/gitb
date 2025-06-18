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

package modules;

import com.google.inject.Provider;
import net.sf.ehcache.Element;
import org.pac4j.play.store.PlayEhCacheStore;
import play.cache.SyncCacheApi;

import javax.inject.Inject;

/**
 * Extension of the PlayEhCacheStore from Pac4J that allows also the setting of a maximum
 * time for sessions apart from the idle timeout.
 */
public class CustomPlayEhCacheStore<K, O> extends PlayEhCacheStore<K, O> {

    private int maxTimeout = 0;

    @Inject
    public CustomPlayEhCacheStore(SyncCacheApi cacheApi) {
        super(cacheApi);
    }

    public CustomPlayEhCacheStore(Provider<SyncCacheApi> cacheProvider) {
        super(cacheProvider);
    }

    @Override
    protected void internalSet(final K key, final O value) {
        final Element e = new Element(computeKey(key), value);
        e.setTimeToIdle(getTimeout());
        e.setTimeToLive(getMaxTimeout());
        getEhcache().put(e);
    }

    @Override
    public String toString() {
        return "CustomPlayEhCacheStore(cache=" + getCache() + ", idleTimeout=" + getTimeout() + ", maxTimeout=" + getMaxTimeout() + ", ehcache=getEhcache()";
    }

    public int getMaxTimeout() {
        return maxTimeout;
    }

    public void setMaxTimeout(int maxTimeOut) {
        this.maxTimeout = maxTimeOut;
    }
}
