# KPing: Production-Ready Uptime Monitoring

KPing is a robust, full-stack uptime monitoring system designed to track the health and performance of your websites and APIs. It provides real-time status updates, historical performance distribution, and detailed response time analytics.

## 🚀 Features

- **Concurrent Monitoring**: High-performance backend using Kotlin Coroutines for non-blocking status checks.
- **Modern Dashboard**: A sleek, dark-themed Next.js dashboard with responsive layouts and interactive components.
- **Analytics & Graphs**: Historical response time visualization using Chart.js.
- **Status Badges**: Real-time "UP/DOWN" status indicators with micro-animations.
- **Full-Stack Containerization**: Simplified deployment using Docker and Docker Compose.
- **Nginx Reverse Proxy**: Efficient routing and security for frontend and backend communication.

## 🛠️ Tech Stack

### Backend
- **Kotlin & Ktor**: Modern, asynchronous framework for building powerful APIs.
- **Exposed ORM**: Type-safe database access for Kotlin.
- **PostgreSQL**: Reliable relational data storage.
- **HikariCP**: High-performance JDBC connection pooling.
- **Kotlinx Serialization**: JSON processing for REST communication.

### Frontend
- **Next.js 15+**: React framework for high-performance web applications.
- **TypeScript**: Static typing for robust frontend logic.
- **TailwindCSS**: Utilty-first CSS for premium, responsive designs.
- **Chart.js**: Dynamic data visualization for response time trends.
- **Lucide-React**: Beautiful, consistent iconography.

## 📁 Project Structure

```text
kping/
├── backend/            # Kotlin/Ktor API & Scheduler
│   ├── src/            # Source code (routes, services, models)
│   ├── build.gradle.kts # Dependency management
│   └── Dockerfile      # Backend container config
├── frontend/           # Next.js Application
│   ├── src/app         # App router (pages & layouts)
│   ├── src/components  # UI Components
│   ├── src/hooks       # Custom React hooks for API interaction
│   └── Dockerfile      # Frontend container config
├── infra/              # Infrastructure configuration
│   ├── nginx.conf      # Reverse proxy configuration
│   └── init.sql        # Database schema initialization
└── docker-compose.yml  # Orchestration for all services
```

## 🚥 Getting Started

### Prerequisites
- [Docker](https://www.docker.com/get-started)
- [Docker Compose](https://docs.docker.com/compose/install/)

### Installation & Run

1.  **Clone the repository**:
    ```bash
    git clone https://github.com/your-username/kping.git
    cd kping
    ```

2.  **Start the system**:
    ```bash
    docker-compose up --build -d
    ```

3.  **Access the application**:
    - **Dashboard**: [http://localhost](http://localhost)
    - **API Health**: [http://localhost/api/health](http://localhost/api/health)

## 📡 API Endpoints

| Method | Endpoint | Description |
| :--- | :--- | :--- |
| `POST` | `/api/monitors` | Create a new monitor |
| `GET` | `/api/monitors` | List all monitors |
| `GET` | `/api/monitors/{id}/status` | Get status history for a monitor |
| `DELETE` | `/api/monitors/{id}` | Remove a monitor |

## 🛡️ Security & Performance

- **Graceful Error Handling**: The monitor worker handles timeouts and SSL errors without interrupting other checks.
- **Request Optimization**: Uses Next.js standalone output and multi-stage Docker builds for minimal footprint.
- **Non-blocking IO**: Built entirely on coroutines and Ktor's Netty engine to handle many concurrent checks with minimal resources.

---

Built with ❤️ by Ayush Katre The Full Stack Engineer.
