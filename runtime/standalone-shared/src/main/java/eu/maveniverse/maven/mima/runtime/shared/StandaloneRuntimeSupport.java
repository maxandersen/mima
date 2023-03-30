package eu.maveniverse.maven.mima.runtime.shared;

import eu.maveniverse.maven.mima.context.Context;
import eu.maveniverse.maven.mima.context.ContextOverrides;
import eu.maveniverse.maven.mima.context.RuntimeSupport;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.apache.maven.repository.internal.MavenRepositorySystemUtils;
import org.apache.maven.settings.Activation;
import org.apache.maven.settings.Mirror;
import org.apache.maven.settings.Profile;
import org.apache.maven.settings.Proxy;
import org.apache.maven.settings.Server;
import org.apache.maven.settings.Settings;
import org.apache.maven.settings.building.DefaultSettingsBuildingRequest;
import org.apache.maven.settings.building.SettingsBuilder;
import org.apache.maven.settings.building.SettingsBuildingException;
import org.apache.maven.settings.crypto.DefaultSettingsDecryptionRequest;
import org.apache.maven.settings.crypto.SettingsDecrypter;
import org.apache.maven.settings.crypto.SettingsDecryptionResult;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.eclipse.aether.ConfigurationProperties;
import org.eclipse.aether.DefaultRepositoryCache;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.util.repository.AuthenticationBuilder;
import org.eclipse.aether.util.repository.DefaultAuthenticationSelector;
import org.eclipse.aether.util.repository.DefaultMirrorSelector;
import org.eclipse.aether.util.repository.DefaultProxySelector;
import org.eclipse.aether.util.repository.SimpleResolutionErrorPolicy;

public abstract class StandaloneRuntimeSupport extends RuntimeSupport {
    protected StandaloneRuntimeSupport(String name, int priority, boolean managedRepositorySystem) {
        super(name, priority, managedRepositorySystem);
    }

    protected static Context buildContext(
            boolean managedRepositorySystem,
            ContextOverrides overrides,
            RepositorySystem repositorySystem,
            SettingsBuilder settingsBuilder,
            SettingsDecrypter settingsDecrypter) {
        try {
            Settings settings = newEffectiveSettings(overrides, settingsBuilder, settingsDecrypter);
            DefaultRepositorySystemSession session = newRepositorySession(overrides, repositorySystem, settings);
            ArrayList<RemoteRepository> remoteRepositories = new ArrayList<>();
            if (overrides.getRepositories() != null) {
                remoteRepositories.addAll(overrides.getRepositories());
            } else {
                remoteRepositories.add(
                        new RemoteRepository.Builder("central", "default", "https://repo.maven.apache.org/maven2/")
                                .build());
            }
            return new Context(managedRepositorySystem, repositorySystem, session, remoteRepositories);
        } catch (Exception e) {
            throw new IllegalStateException("Cannot create context from scratch", e);
        }
    }

    protected static Settings newEffectiveSettings(
            ContextOverrides overrides, SettingsBuilder settingsBuilder, SettingsDecrypter settingsDecrypter)
            throws SettingsBuildingException {
        DefaultSettingsBuildingRequest settingsBuilderRequest = new DefaultSettingsBuildingRequest();
        settingsBuilderRequest.setSystemProperties(System.getProperties());
        if (overrides.getUserProperties() != null) {
            settingsBuilderRequest.getUserProperties().putAll(overrides.getUserProperties());
        }

        if (overrides.isWithUserSettings()) {
            // find the settings
            Path settingsFile = overrides.getSettingsXml();
            if (settingsFile == null) {
                Path userSettings = Paths.get(System.getProperty("user.home"), ".m2", "settings.xml");
                if (Files.isRegularFile(userSettings)) {
                    settingsFile = userSettings.toAbsolutePath();
                }
            }
            if (settingsFile != null) {
                settingsBuilderRequest.setGlobalSettingsFile(settingsFile.toFile());
            }
        }
        Settings effectiveSettings =
                settingsBuilder.build(settingsBuilderRequest).getEffectiveSettings();

        DefaultSettingsDecryptionRequest decrypt = new DefaultSettingsDecryptionRequest();
        decrypt.setProxies(effectiveSettings.getProxies());
        decrypt.setServers(effectiveSettings.getServers());
        SettingsDecryptionResult decrypted = settingsDecrypter.decrypt(decrypt);

        if (!decrypted.getProblems().isEmpty()) {
            throw new SettingsBuildingException(decrypted.getProblems());
        }

        return effectiveSettings;
    }

    protected static List<Profile> activeProfiles(Settings settings) {
        ArrayList<Profile> result = new ArrayList<>();
        for (Profile profile : settings.getProfiles()) {
            Activation activation = profile.getActivation();
            if (activation != null) {
                if (activation.isActiveByDefault()) {
                    result.add(profile);
                }
            }
        }
        return result;
    }

    protected static DefaultRepositorySystemSession newRepositorySession(
            ContextOverrides overrides, RepositorySystem repositorySystem, Settings settings) {
        DefaultRepositorySystemSession session = MavenRepositorySystemUtils.newSession();

        session.setCache(new DefaultRepositoryCache());

        LinkedHashMap<Object, Object> configProps = new LinkedHashMap<>();
        configProps.put(ConfigurationProperties.USER_AGENT, getUserAgent());
        configProps.put(ConfigurationProperties.INTERACTIVE, false);
        configProps.put("maven.startTime", new Date());
        // First add properties populated from settings.xml
        List<Profile> activeProfiles = activeProfiles(settings);
        for (Profile profile : activeProfiles) {
            configProps.putAll(profile.getProperties());
        }
        // Resolver's ConfigUtils solely rely on config properties, that is why we need to add both here as well.
        configProps.putAll(System.getProperties());
        if (overrides.getUserProperties() != null) {
            configProps.putAll(overrides.getUserProperties());
        }

        session.setOffline(overrides.isOffline());

        customizeChecksumPolicy(overrides, session);

        customizeSnapshotUpdatePolicy(overrides, session);

        // we should not interfere with "real Maven"
        session.setResolutionErrorPolicy(new SimpleResolutionErrorPolicy(false, false));

        DefaultMirrorSelector mirrorSelector = new DefaultMirrorSelector();
        for (Mirror mirror : settings.getMirrors()) {
            mirrorSelector.add(
                    mirror.getId(),
                    mirror.getUrl(),
                    mirror.getLayout(),
                    false,
                    mirror.isBlocked(),
                    mirror.getMirrorOf(),
                    mirror.getMirrorOfLayouts());
        }
        session.setMirrorSelector(mirrorSelector);

        DefaultProxySelector proxySelector = new DefaultProxySelector();
        for (Proxy proxy : settings.getProxies()) {
            AuthenticationBuilder authBuilder = new AuthenticationBuilder();
            authBuilder.addUsername(proxy.getUsername()).addPassword(proxy.getPassword());
            proxySelector.add(
                    new org.eclipse.aether.repository.Proxy(
                            proxy.getProtocol(), proxy.getHost(), proxy.getPort(), authBuilder.build()),
                    proxy.getNonProxyHosts());
        }
        session.setProxySelector(proxySelector);

        DefaultAuthenticationSelector authSelector = new DefaultAuthenticationSelector();
        for (Server server : settings.getServers()) {
            AuthenticationBuilder authBuilder = new AuthenticationBuilder();
            authBuilder.addUsername(server.getUsername()).addPassword(server.getPassword());
            authBuilder.addPrivateKey(server.getPrivateKey(), server.getPassphrase());
            authSelector.add(server.getId(), authBuilder.build());

            if (server.getConfiguration() != null) {
                Xpp3Dom dom = (Xpp3Dom) server.getConfiguration();
                for (int i = dom.getChildCount() - 1; i >= 0; i--) {
                    Xpp3Dom child = dom.getChild(i);
                    if ("wagonProvider".equals(child.getName())) {
                        dom.removeChild(i);
                    }
                }

                // Translate to proper resolver configuration properties as well (as Plexus XML above is Wagon specific
                // only), but support only configuration/httpConfiguration/all, see
                // https://maven.apache.org/guides/mini/guide-http-settings.html
                Map<String, String> headers = null;
                Integer connectTimeout = null;
                Integer requestTimeout = null;

                Xpp3Dom httpHeaders = dom.getChild("httpHeaders");
                if (httpHeaders != null) {
                    Xpp3Dom[] properties = httpHeaders.getChildren("property");
                    if (properties != null && properties.length > 0) {
                        headers = new HashMap<>();
                        for (Xpp3Dom property : properties) {
                            headers.put(
                                    property.getChild("name").getValue(),
                                    property.getChild("value").getValue());
                        }
                    }
                }

                Xpp3Dom connectTimeoutXml = dom.getChild("connectTimeout");
                if (connectTimeoutXml != null) {
                    connectTimeout = Integer.parseInt(connectTimeoutXml.getValue());
                }

                Xpp3Dom requestTimeoutXml = dom.getChild("requestTimeout");
                if (requestTimeoutXml != null) {
                    requestTimeout = Integer.parseInt(requestTimeoutXml.getValue());
                }

                // org.eclipse.aether.ConfigurationProperties.HTTP_HEADERS => Map<String, String>
                if (headers != null) {
                    configProps.put(ConfigurationProperties.HTTP_HEADERS + "." + server.getId(), headers);
                }
                // org.eclipse.aether.ConfigurationProperties.CONNECT_TIMEOUT => int
                if (connectTimeout != null) {
                    configProps.put(ConfigurationProperties.CONNECT_TIMEOUT + "." + server.getId(), connectTimeout);
                }
                // org.eclipse.aether.ConfigurationProperties.REQUEST_TIMEOUT => int
                if (requestTimeout != null) {
                    configProps.put(ConfigurationProperties.REQUEST_TIMEOUT + "." + server.getId(), requestTimeout);
                }
            }

            configProps.put("aether.connector.perms.fileMode." + server.getId(), server.getFilePermissions());
            configProps.put("aether.connector.perms.dirMode." + server.getId(), server.getDirectoryPermissions());
        }
        session.setAuthenticationSelector(authSelector);

        session.setUserProperties(
                overrides.getUserProperties() != null ? overrides.getUserProperties() : new HashMap<>());
        session.setSystemProperties(System.getProperties());
        session.setConfigProperties(configProps);

        if (overrides.getTransferListener() != null) {
            session.setTransferListener(overrides.getTransferListener());
        }
        if (overrides.getRepositoryListener() != null) {
            session.setRepositoryListener(overrides.getRepositoryListener());
        }

        Path localRepoPath = null;
        if (settings.getLocalRepository() != null) {
            localRepoPath = Paths.get(settings.getLocalRepository());
        }
        if (localRepoPath == null) {
            localRepoPath = Paths.get(System.getProperty("user.home"), ".m2", "repository");
        }

        newLocalRepositoryManager(localRepoPath, repositorySystem, session);
        customizeLocalRepositoryManager(overrides, repositorySystem, session);

        return session;
    }

    protected static String getUserAgent() {
        String version = "incubation";
        return "MIMA/" + version + " (Java " + System.getProperty("java.version") + "; " + System.getProperty("os.name")
                + " " + System.getProperty("os.version") + ")";
    }
}