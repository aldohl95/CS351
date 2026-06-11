# Service Request Tracker
A full-stack application for submitting, tracking, and managing service requests. Built with a Spring Boot backend and a React frontend.

---

## Structure

```
service-request-tracker/
├── frontend/   - React frontend (npm + Vite)
├── backend/    - Java backend (Spring Boot + Maven)
└── data/       - JSON file storage (auto-created on first request)
```

---

## What the application does

The backend exposes a REST API under `/api/requests` that supports:

- Creating a new service request
- Retrieving all requests or filtering by status, priority, requester, or keyword
- Viewing a single request by ID
- Updating the status or priority of a request
- Adding comments or notes to a request
- Viewing the full history of changes made to a request

The frontend provides a browser-based interface to perform all of the above without using the API directly.

All data is persisted in a local JSON file. No database setup is required.

---

## Prerequisites

You need all of the following installed before you can run the project:

- **Node.js + npm** for the frontend
- **Java 17 JDK** for the backend
- **Apache Maven** for the backend build and run commands

Verify your tools after installation:

```
node -v
npm -v
java -version
mvn -version
```

If all four commands work, your machine is ready to run the project.

---

## Install prerequisites on macOS

### 1) Install Node.js and npm

Go to the official Node.js download page: https://nodejs.org/en/download

Download the current LTS macOS installer (`.pkg`), run the installer, then open a new Terminal window and verify:

```
node -v
npm -v
```

### 2) Install Java 17 JDK

Go to the Oracle JDK 17 download page and download the macOS installer for your machine:

- Apple Silicon (M1/M2/M3): macOS aarch64
- Intel Mac: macOS x64

Open the downloaded `.dmg`, run the installer, then verify:

```
java -version
```

### 3) Install Apache Maven

**Option A: Homebrew (easiest if you already use Homebrew)**

```
brew install maven
```

**Option B: Manual install**

Download the latest Apache Maven binary `.tar.gz` archive and extract it somewhere permanent, for example `/Users/<your-user>/tools/apache-maven`. Add Maven's bin folder to your shell profile:

```
# Add to ~/.zshrc
export MAVEN_HOME="$HOME/tools/apache-maven"
export PATH="$MAVEN_HOME/bin:$PATH"
```

Reload your shell:

```
source ~/.zshrc
```

Verify:

```
mvn -version
```

---

## Install prerequisites on Windows

### 1) Install Node.js and npm

Go to https://nodejs.org/en/download, download the current LTS Windows installer (`.msi`), and run it. Open a new Command Prompt or PowerShell window and verify:

```
node -v
npm -v
```

### 2) Install Java 17 JDK

Go to the Oracle JDK 17 download page and download the Windows x64 installer (`.exe`). Run the installer, open a new Command Prompt or PowerShell window, and verify:

```
java -version
```

### 3) Install Apache Maven

**Option A: Manual install (recommended on Windows)**

Download the latest Apache Maven binary zip archive and extract it to a permanent folder, for example `C:\tools\apache-maven`. Add `C:\tools\apache-maven\bin` to your system Path. Open a new Command Prompt or PowerShell window and verify:

```
mvn -version
```

**Option B: Winget**

```
winget install Apache.Maven
```

Verify:

```
mvn -version
```

---

## Install prerequisites on Linux

### 1) Install Node.js and npm

**Option A: Package manager**

```
# Ubuntu / Debian
sudo apt update
sudo apt install -y nodejs npm

# Fedora
sudo dnf install -y nodejs npm
```

**Option B: Official Node.js binaries**

Go to https://nodejs.org/en/download, download the current LTS Linux binary archive for your architecture, extract it, and add its `bin` folder to your PATH.

Verify:

```
node -v
npm -v
```

### 2) Install Java 17 JDK

**Option A: Package manager**

```
# Ubuntu / Debian
sudo apt update
sudo apt install -y openjdk-17-jdk

# Fedora
sudo dnf install -y java-17-openjdk-devel
```

**Option B: Official Oracle packages**

Go to the Oracle JDK 17 download page and download the `.deb`, `.rpm`, or `.tar.gz` for your platform.

Verify:

```
java -version
```

### 3) Install Apache Maven

**Option A: Package manager**

```
# Ubuntu / Debian
sudo apt update
sudo apt install -y maven

# Fedora
sudo dnf install -y maven
```

**Option B: Manual install**

Download the latest Apache Maven binary `.tar.gz` archive, extract it to a permanent folder such as `$HOME/tools/apache-maven`, and add its `bin` directory to your PATH:

```
export MAVEN_HOME="$HOME/tools/apache-maven"
export PATH="$MAVEN_HOME/bin:$PATH"
```

Reload your shell:

```
source ~/.bashrc
```

Verify:

```
mvn -version
```

---

## Run backend tests and coverage

From the `backend/` directory:

```
mvn test
```

This command will:

- Run all backend unit tests
- Generate a JaCoCo coverage report
- Fail the build if overall instruction coverage is below 80%

If coverage is too low, Maven will fail with this message:

```
Don't forget it's mandatory to maintain 80% code coverage
```

After a successful run, open the HTML coverage report here:

```
backend/target/site/jacoco/index.html
```

---

## Run the backend

```
cd backend
mvn spring-boot:run
```

The backend runs on: http://localhost:8080

Verify it is running by opening http://localhost:8080/api/requests in a browser. You should see an empty JSON array `[]` on a fresh install.

---

## Run the frontend

Open a second terminal:

```
cd frontend
npm install
npm run dev
```

The frontend runs on: http://localhost:5173

The Vite dev server proxies `/api/*` requests to the backend automatically. Both servers must be running at the same time.

---

## Suggested workflow

1. Clone the repository
2. Install Node.js, npm, Java 17, and Maven
3. Verify the starter application works locally
4. Create a feature branch
5. Implement project requirements incrementally
6. Add unit tests as features are delivered
7. Use pull requests for meaningful changes

---

## API reference

### Create a request
```
POST /api/requests
Body: { "title": "...", "description": "...", "createdBy": "..." }
```

### List all requests
```
GET /api/requests
```

### Filter / search requests
```
GET /api/requests?status=OPEN
GET /api/requests?priority=HIGH
GET /api/requests?requester=jane
GET /api/requests?keyword=login
```

Filters can be combined. Multiple parameters are applied as AND conditions.

### Get a single request
```
GET /api/requests/{id}
```

### Update status or priority
```
PATCH /api/requests/{id}
Body: { "status": "IN_PROGRESS", "changedBy": "username" }
```

### Add a comment
```
POST /api/requests/{id}/comments
Body: { "content": "...", "author": "username" }
```

---

## Data storage

All requests are stored in `data/requests.json` in the project root. This file is created automatically on the first request submission. To reset the application to a clean state, stop the backend and delete this file.
