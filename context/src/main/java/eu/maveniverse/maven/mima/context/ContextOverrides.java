package eu.maveniverse.maven.mima.context;

import static java.util.Objects.requireNonNull;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.eclipse.aether.RepositoryListener;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.transfer.TransferListener;

public final class ContextOverrides {

    public static final RemoteRepository CENTRAL =
            new RemoteRepository.Builder("central", "default", "https://repo.maven.apache.org/maven2/").build();

    public enum SnapshotUpdatePolicy {
        ALWAYS,
        NEVER
    }

    public enum ChecksumPolicy {
        FAIL,
        WARN,
        IGNORE
    }

    private final Map<String, String> userProperties;

    private final Map<String, Object> configProperties;

    private final List<RemoteRepository> repositories;

    private final boolean offline;

    private final Path localRepository;

    private final SnapshotUpdatePolicy snapshotUpdatePolicy;

    private final ChecksumPolicy checksumPolicy;

    private final boolean withUserSettings;

    private final Path settingsXml;

    private final RepositoryListener repositoryListener;

    private final TransferListener transferListener;

    private ContextOverrides(Builder builder) {
        this.userProperties = builder.userProperties;
        this.configProperties = builder.configProperties;
        this.repositories = builder.repositories;
        this.offline = builder.offline;
        this.localRepository = builder.localRepository;
        this.snapshotUpdatePolicy = builder.snapshotUpdatePolicy;
        this.checksumPolicy = builder.checksumPolicy;
        this.withUserSettings = builder.withUserSettings;
        this.settingsXml = builder.settingsXml;
        this.repositoryListener = builder.repositoryListener;
        this.transferListener = builder.transferListener;
    }

    public Map<String, String> getUserProperties() {
        return userProperties;
    }

    public Map<String, Object> getConfigProperties() {
        return configProperties;
    }

    public List<RemoteRepository> getRepositories() {
        return repositories;
    }

    public boolean isOffline() {
        return offline;
    }

    public Path getLocalRepository() {
        return localRepository;
    }

    public SnapshotUpdatePolicy getSnapshotUpdatePolicy() {
        return snapshotUpdatePolicy;
    }

    public ChecksumPolicy getChecksumPolicy() {
        return checksumPolicy;
    }

    public boolean isWithUserSettings() {
        return withUserSettings;
    }

    public Path getSettingsXml() {
        return settingsXml;
    }

    public RepositoryListener getRepositoryListener() {
        return repositoryListener;
    }

    public TransferListener getTransferListener() {
        return transferListener;
    }

    public static final class Builder {
        private Map<String, String> userProperties;

        private Map<String, Object> configProperties;

        private List<RemoteRepository> repositories;

        private boolean offline;

        private Path localRepository;

        private SnapshotUpdatePolicy snapshotUpdatePolicy;

        private ChecksumPolicy checksumPolicy;

        private boolean withUserSettings;

        private Path settingsXml;

        private RepositoryListener repositoryListener;

        private TransferListener transferListener;

        public static Builder create() {
            return new Builder();
        }

        public Builder userProperties(Map<String, String> userProperties) {
            if (userProperties != null) {
                this.userProperties = new HashMap<>(userProperties);
            } else {
                this.userProperties = null;
            }
            return this;
        }

        public Builder setUserProperty(String name, String value) {
            requireNonNull(name);
            if (this.userProperties == null) {
                this.userProperties = new HashMap<>();
            }
            this.userProperties.put(name, value);
            return this;
        }

        public Builder configProperties(Map<String, Object> configProperties) {
            if (configProperties != null) {
                this.configProperties = new HashMap<>(configProperties);
            } else {
                this.configProperties = null;
            }
            return this;
        }

        public Builder setConfigProperty(String name, Object value) {
            requireNonNull(name);
            if (this.configProperties == null) {
                this.configProperties = new HashMap<>();
            }
            this.configProperties.put(name, value);
            return this;
        }

        public Builder repositories(List<RemoteRepository> repositories) {
            if (repositories != null) {
                this.repositories = new ArrayList<>(repositories);
            } else {
                this.repositories = null;
            }
            return this;
        }

        public Builder addRepository(RemoteRepository repository) {
            requireNonNull(repository);
            if (this.repositories == null) {
                this.repositories = new ArrayList<>();
            }
            this.repositories.add(repository);
            return this;
        }

        public Builder offline(boolean offline) {
            this.offline = offline;
            return this;
        }

        public Builder localRepository(Path localRepository) {
            this.localRepository = localRepository;
            return this;
        }

        public Builder snapshotUpdatePolicy(SnapshotUpdatePolicy snapshotUpdatePolicy) {
            this.snapshotUpdatePolicy = snapshotUpdatePolicy;
            return this;
        }

        public Builder checksumPolicy(ChecksumPolicy checksumPolicy) {
            this.checksumPolicy = checksumPolicy;
            return this;
        }

        public Builder withUserSettings(boolean withUserSettings) {
            this.withUserSettings = withUserSettings;
            return this;
        }

        public Builder settingsXml(Path settingsXml) {
            this.settingsXml = settingsXml;
            return this;
        }

        public Builder repositoryListener(RepositoryListener repositoryListener) {
            this.repositoryListener = repositoryListener;
            return this;
        }

        public Builder transferListener(TransferListener transferListener) {
            this.transferListener = transferListener;
            return this;
        }

        public ContextOverrides build() {
            return new ContextOverrides(this);
        }
    }
}
