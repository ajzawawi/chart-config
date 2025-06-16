## Temporal Helm Chart Values Documentation (Markdown Table Format)

This document explains the configuration fields available in the `values.yaml` file for deploying [Temporal](https://temporal.io) using the Helm chart. Each field is categorized by its origin:

- **[K8s]**: Native Kubernetes construct
- **[Helm]**: Standard Helm construct or Helm chart-specific
- **[Temporal]**: Temporal-specific configuration

---

### Top-Level Fields

| Field | Type | Description | Required |
|-------|------|-------------|----------|
| `nameOverride` | Helm | Override the default chart name | No |
| `fullnameOverride` | Helm | Fully override the generated release name | No |
| `debug` | Helm | Enable Helm debug mode (e.g., disables hook-delete-policy) | No |
| `imagePullSecrets` | K8s | Secrets used to pull images from private registries | No |
| `serviceAccount.create` | K8s/Helm | Whether to create a new service account | No |
| `serviceAccount.name` | K8s/Helm | Name of the service account to use | No |
| `serviceAccount.extraAnnotations` | K8s | Annotations to attach to the service account | No |
| `additionalAnnotations` | K8s | Annotations to apply globally | No |
| `additionalLabels` | K8s | Labels to apply globally | No |

### `server:` Block

| Field | Type | Description | Required |
|-------|------|-------------|----------|
| `server.enabled` | Temporal | Enable the Temporal server component | Yes |
| `server.image.repository` | Temporal | Docker image for Temporal server | Yes |
| `server.image.tag` | Temporal | Image tag | Yes |
| `server.replicaCount` | Temporal | Number of Temporal server pods | No |
| `server.metrics.annotations.enabled` | Temporal | Add Prometheus annotations | No |
| `server.metrics.tags` | Temporal | Tags to include in metrics | No |
| `server.metrics.excludeTags` | Temporal | Tags to exclude from metrics | No |
| `server.metrics.serviceMonitor.enabled` | Temporal | Enable Prometheus ServiceMonitor | No |
| `server.resources` | K8s | Pod resource requests/limits | No |
| `server.nodeSelector` | K8s | Node selector for pod scheduling | No |
| `server.tolerations` | K8s | Tolerations for pod scheduling | No |
| `server.affinity` | K8s | Affinity rules for pod scheduling | No |
| `server.securityContext.fsGroup` | K8s | File system group ID | No |
| `server.securityContext.runAsUser` | K8s | UID to run container as | No |
| `server.additionalVolumes` | K8s | Extra volumes to mount | No |
| `server.additionalVolumeMounts` | K8s | Extra volume mounts | No |
| `server.additionalEnv` | K8s | Additional environment variables | No |
| `server.additionalInitContainers` | K8s | Additional init containers | No |

### `server.config:` Block

| Field | Type | Description | Required |
|-------|------|-------------|----------|
| `logLevel` | Temporal | Log level for server components | No |
| `numHistoryShards` | Temporal | Number of history shards | Yes |
| `tls` | Temporal | TLS configuration (frontend/internode) | No |
| `authorization` | Temporal | JWT authorization config | No |

### `persistence:` Block

| Field | Type | Description | Required |
|-------|------|-------------|----------|
| `defaultStore` | Temporal | Default data store name | Yes |
| `additionalStores` | Temporal | Additional named stores | No |

#### SQL Persistence

| Field | Type | Description | Required |
|-------|------|-------------|----------|
| `sql.driver` | Temporal | SQL driver name (e.g., mysql8, postgres12) | Yes |
| `sql.host`, `sql.port` | Temporal | Host and port for SQL DB | Yes |
| `sql.database` | Temporal | Name of SQL database | Yes |
| `sql.user`, `sql.password` | Temporal | Credentials to access the DB | Yes (or use secret) |
| `sql.existingSecret`, `sql.secretName` | K8s/Temporal | Kubernetes Secret containing the password | No (optional if password is set) |
| `sql.maxConns`, `sql.maxIdleConns`, `sql.maxConnLifetime` | Temporal | DB connection pool settings | No |

#### Cassandra Persistence

| Field | Type | Description | Required |
|-------|------|-------------|----------|
| `cassandra.hosts` | Temporal | List of Cassandra hosts | Yes (if Cassandra used) |
| `cassandra.keyspace` | Temporal | Keyspace name | Yes |
| `cassandra.user`, `cassandra.password` | Temporal | Credentials to access Cassandra | Yes (or use secret) |
| `cassandra.existingSecret` | K8s/Temporal | Kubernetes Secret containing the password | No (optional if password is set) |
| `cassandra.replicationFactor` | Temporal | Replication factor in Cassandra | No |

### Other Blocks

| Field | Type | Description | Required |
|-------|------|-------------|----------|
| `namespaces.create` | Temporal | Automatically create Temporal namespaces | No |
| `frontend`, `history`, `matching`, `worker`, `internalFrontend` | Temporal/K8s | Service-specific configuration | No |
| `admintools.enabled` | Temporal | Deploy admin tools (CLI pod) | No |
| `web.enabled` | Temporal | Deploy Temporal Web UI | No |
| `schema.createDatabase`, `setup`, `update` | Temporal | DB and schema lifecycle jobs | No |
| `elasticsearch.enabled` | Temporal | Enable Elasticsearch for visibility | No |
| `elasticsearch.host`, `port`, `username`, `password` | Temporal | Elasticsearch connection settings | Yes (if ES enabled) |
| `prometheus.enabled` | Helm | Deploy Prometheus server | No |
| `grafana.enabled` | Helm | Deploy Grafana dashboards | No |
| `cassandra.enabled` | Temporal | Deploy Cassandra instance (optional) | No |
| `mysql.enabled` | Temporal | Deploy MySQL instance (optional) | No |
