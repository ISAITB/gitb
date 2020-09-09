package modules;

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
