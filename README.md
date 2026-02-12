# FogFort

FogFort forms the backbone of the NebulOuS Executionware layer, translating optimized deployment plans into infrastructure provisioning and orchestration actions across heterogeneous environments—public clouds, private data centers, and edge devices.

The platform directly interacts with infrastructure APIs from various cloud, on-premise, and edge providers to deploy, configure, and manage computing resources. Through integration with the REST API, FogFort handles infrastructure resource retrieval based on user-defined constraints (CPU, RAM, region, etc.), constructs deployment jobs with per-task node selection strategies, manages node candidate caching and reuse, and initiates cluster setup and workload deployment.

A comprehensive REST API defines the complete deployment lifecycle, from cloud provider registration to application orchestration, with endpoints integrated into the NebulOuS platform's GUI for seamless infrastructure management and deployment operations.



## Disclaimer

**This component is currently a work in progress.** 

FogFort is being developed to replace the [Scheduler Abstraction Layer (SAL)](https://github.com/eu-nebulous/sal) and ProActive Deployment Manager with a fully open-source alternative licensed under the **Mozilla Public License 2.0 (MPL 2.0)**. This licensing change enables commercial use of the NebulOuS platform without requiring proprietary licenses, making it more accessible to organizations and enterprises while maintaining compatibility with existing NebulOuS deployments.

The component is still under construction. Currently, it handles the following aspects:

- **API Compatibility**: Near-identical API to SAL, ensuring full compatibility with NebulOuS.
- **Cloud Account Management**: Registration of AWS cloud accounts and fetching available node candidates from these providers.
- **Node Candidate Querying**: Querying previously fetched node candidates with filtering by various criteria (CPU, RAM, region, etc.).
- **Cluster Deployment**: Defining and deploying clusters by creating nodes in AWS and executing the appropriate scripts to configure clusters as required by NebulOuS. Uses scripts from [sal-scripts](https://github.com/eu-nebulous/sal-scripts).
- **Job Tracking**: Web-based user interface for tracking job executions and monitoring deployment status.

Currently, the component is missing:

- **Testing**: Comprehensive test coverage for existing functionalities.
- **Resource Cleanup**: Deletion of cloud providers and proper resource deallocation.
- **Multi-Cloud Support**: Integration with additional cloud providers (OpenStack, GCP, Azure, etc.). This can be easily achieved by implementing the `CloudProvider` interface. Currently, only AWS is supported.
- **Edge Computing**: Support for handling edge resources and edge device management.
- **Security**: Proper user authentication, authorization, and credential management.


## Intended Features

- **Multi-Cloud Support**: Manage resources across multiple cloud providers (AWS, OpenStack, Azure)
- **Kubernetes Cluster Management**: Deploy and manage Kubernetes clusters with support for kubectl, KubeVela, and Helm
- **Node Candidate Management**: Discover and manage compute nodes across cloud environments
- **Resource Management**: Manage cloud images, hardware configurations, and deployments
- **REST API**: Comprehensive REST API with Swagger documentation
- **Web GUI**: Web-based interface for managing clouds, clusters, and jobs

## Technology Stack

- **Java 21**: Modern Java runtime
- **Spring Boot 4.0**: Application framework
- **Spring Data JPA**: Data persistence layer
- **Hibernate**: ORM framework
- **HSQLDB**: Embedded database
- **AWS SDK**: EC2 cloud provider integration
- **JSch**: SSH connectivity for remote operations

## Building and Running

### Prerequisites

- Java 21 or higher
- Gradle (or use the included Gradle wrapper)

### Build

```bash
./gradlew build
```

### Run

```bash
./gradlew bootRun
```

The application will start on port 8888 by default. Access the web GUI at `http://localhost:8888/gui/`.

<img  alt="image" src="https://github.com/user-attachments/assets/d7bc62fb-5aa4-46de-b922-012f195cc5fa" />


### Run Tests

```bash
./gradlew test
```

## Configuration

Configuration is managed through `src/main/resources/application.properties`:

- `server.port`: Server port (default: 8888)
- `spring.jpa.hibernate.ddl-auto`: Database schema management (update, create, validate, etc.)
- `fogfort.output.log.directory`: Log file directory
- `fogfort.data.home`: Data directory
- `fogfort.security.disabled`: Security settings

## API Endpoints

The application provides REST endpoints for:

- **Cloud Management** (`/sal/cloud`): Add, remove, refresh, and query cloud configurations
- **Cluster Management** (`/sal/cluster`): Deploy and manage Kubernetes clusters
- **Node Candidates** (`/sal/nodecandidate`): Discover and manage compute nodes
- **Jobs** (`/sal/job`): Manage deployment and execution jobs
- **Edge** (`/sal/edge`): Edge computing resources
- **Users** (`/sal/user`): User management

API documentation is available via Swagger when the application is running.

## Project Structure

```
src/
├── main/
│   ├── java/eu/nebulouscloud/fogfort/
│   │   ├── cloud/          # Cloud provider implementations
│   │   ├── config/         # Spring configuration
│   │   ├── controller/     # REST controllers
│   │   ├── dto/            # Data transfer objects
│   │   ├── model/          # JPA entities
│   │   ├── repository/     # Data repositories
│   │   ├── service/        # Business logic
│   │   └── util/           # Utility classes
│   └── resources/
│       ├── application.properties
│       └── static/         # Web GUI files
└── test/                   # Test files
```

## License

This project is licensed under the Mozilla Public License 2.0. See the source code headers for details.

