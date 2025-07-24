# Temporal Skeleton

A lightweight starter repository for building and running Temporal workflows and workers, showcasing how to use the shared **Temporal library**.

This repo includes:
- **Sample Temporal Worker** – Registers workflows and activities using the reusable library.
- **Sample `TemporalWorkflowService`** – Bootstraps and executes workflows, with support for dynamic plugin loading or a `WorkflowRegistry` for local development.
- **Gradle + IntelliJ Integration** – Designed for fast developer iteration without requiring manual JAR creation.

---

## Getting Started

### **1. Prerequisites**
- **Java 11+ or Scala-compatible JDK**
- **Gradle 8+**
- **Temporal Server** (run locally using Docker):
  ```bash
  temporal server start-dev
  ```

---

### **2. Clone and Build**
```bash
git clone https://github.com/your-org/temporal-skeleton.git
cd temporal-skeleton
./gradlew build
```

---

### **3. Running Locally**
This skeleton includes a sample runner that uses a `WorkflowRegistry` for local testing:

```bash
./gradlew run
```

Or directly from IntelliJ:
- Run `DevRunner` (uses `TemporalWorkflowService(Some(DevWorkflowRegistry))`).

---

### **4. WorkflowRegistry**
To add your own workflows and activities for local development, edit:

```scala
object DevWorkflowRegistry extends WorkflowRegistry {
  override val workflows = List(classOf[MyWorkflowImpl])
  override val activities = List(new MyActivityImpl)
}
```

---

### **5. Production Mode**
In production, `TemporalWorkflowService` automatically loads workflows and activities from JARs using `PluginLoader`:
```scala
TemporalWorkflowService() // No registry needed
```

---

## Repository Structure
```
.
├── build.gradle.kts         # Gradle build config
├── src/main/scala/
│   ├── workers/             # Sample Temporal Worker
│   ├── workflows/           # Sample Workflow Implementations
│   └── DevRunner.scala      # Local entry point
└── README.md
```

---

## Next Steps
- Use this skeleton as a template to create your own Temporal services.
- Explore `TemporalWorkflowService` for advanced configurations.
- Add your custom workflows and activities in `DevWorkflowRegistry`.

---

## License
MIT (or your chosen license)
