//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package org.neo4j.driver;

import java.net.URI;
import java.util.Iterator;

import cn.pandadb.driver.PandaDriver;
import org.neo4j.driver.exceptions.ServiceUnavailableException;
import org.neo4j.driver.internal.DriverFactory;
import org.neo4j.driver.internal.cluster.RoutingSettings;
import org.neo4j.driver.internal.retry.RetrySettings;

public class GraphDatabase {
    private static final String LOGGER_NAME = GraphDatabase.class.getSimpleName();

    public GraphDatabase() {
    }

    public static Driver driver(String uri) {
        return driver(uri, Config.defaultConfig());
    }

    public static Driver driver(URI uri) {
        return driver(uri, Config.defaultConfig());
    }

    public static Driver driver(URI uri, Config config) {
        return driver(uri, AuthTokens.none(), config);
    }

    public static Driver driver(String uri, Config config) {
        return driver(URI.create(uri), config);
    }

    public static Driver driver(String uri, AuthToken authToken) {
        return driver(uri, authToken, Config.defaultConfig());
    }

    public static Driver driver(URI uri, AuthToken authToken) {
        return driver(uri, authToken, Config.defaultConfig());
    }

    public static Driver driver(String uri, AuthToken authToken, Config config) {
        //NOTE: pandadb
        if (uri.startsWith("panda2://")) {
            return PandaDriver.create(uri, authToken, config);
        }
        //NOTE
        return driver(URI.create(uri), authToken, config);
    }

    public static Driver driver(URI uri, AuthToken authToken, Config config) {
        config = getOrDefault(config);
        RoutingSettings routingSettings = config.routingSettings();
        RetrySettings retrySettings = config.retrySettings();
        return (new DriverFactory()).newInstance(uri, authToken, routingSettings, retrySettings, config);
    }

    public static Driver routingDriver(Iterable<URI> routingUris, AuthToken authToken, Config config) {
        assertRoutingUris(routingUris);
        Logger log = createLogger(config);
        Iterator var4 = routingUris.iterator();

        while (var4.hasNext()) {
            URI uri = (URI) var4.next();

            try {
                return driver(uri, authToken, config);
            } catch (ServiceUnavailableException var7) {
                log.warn("Unable to create routing driver for URI: " + uri, var7);
            }
        }

        throw new ServiceUnavailableException("Failed to discover an available server");
    }

    private static void assertRoutingUris(Iterable<URI> uris) {
        Iterator var1 = uris.iterator();

        URI uri;
        do {
            if (!var1.hasNext()) {
                return;
            }

            uri = (URI) var1.next();
        } while ("neo4j".equals(uri.getScheme()));

        throw new IllegalArgumentException("Illegal URI scheme, expected 'neo4j' in '" + uri + "'");
    }

    private static Logger createLogger(Config config) {
        Logging logging = getOrDefault(config).logging();
        return logging.getLog(LOGGER_NAME);
    }

    private static Config getOrDefault(Config config) {
        return config != null ? config : Config.defaultConfig();
    }
}
