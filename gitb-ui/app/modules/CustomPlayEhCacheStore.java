package modules;

import com.google.inject.Provider;
import net.sf.ehcache.Element;
import org.pac4j.core.util.CommonHelper;
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
        return CommonHelper.toNiceString(this.getClass(), "cache", getCache(),
                "idleTimeout", getTimeout(),
                "maxTimeout", getMaxTimeout(),
                "ehcache", getEhcache());
    }

    public int getMaxTimeout() {
        return maxTimeout;
    }

    public void setMaxTimeout(int maxTimeOut) {
        this.maxTimeout = maxTimeOut;
    }
}
