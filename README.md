# ✈️ Flight Search Application

This is a full-stack web application that allows users to search for flights using the **Amadeus REST API**. The application is divided into a **React + TypeScript frontend** and a **Spring Boot + Java backend**, both containerized using **Docker** and orchestrated with **Docker Compose**.

---

## 🧰 Tech Stack

### Frontend
- React
- TypeScript
- Create React App (CRA)
- CSS Modules
- Axios
- Docker + Nginx

### Backend
- Java 21
- Spring Boot
- Spring WebFlux
- Gradle
- Jackson (for JSON parsing)
- Docker

---

## ⚙️ Project Structure
```bash
  /project-root
  │
  ├── backend/ # Java Spring Boot backend
  │ ├── src/
  │ ├── build.gradle
  │ ├── settings.gradle
  │ └── Dockerfile
  │
  ├── frontend/ # React TypeScript frontend
  │ ├── src/
  │ ├── public/
  │ ├── package.json
  │ ├── tsconfig.json
  │ └── Dockerfile
  │
  └── docker-compose.yml # Compose file to run both services
```

---

## 🚀 Getting Started with Docker

### 🐳 Prerequisites
You must have:
- [Docker Desktop](https://www.docker.com/products/docker-desktop) installed
- Git

---

### 🛠️ Build and Run the App

Run the following command from the **root of the project**:

```bash
docker compose up --build
```
This will:

Build the backend using Gradle in a Docker container.
Build the frontend using React scripts inside a Docker container.
Serve the frontend via Nginx and the backend via Spring Boot.
📍 After the build completes:

- 🟢 Frontend: http://localhost:3000
- 🔵 Backend API: http://localhost:8080/api/flights

## 🧹 Stop the App
To stop the containers, run:
```bash
docker compose down
```
## 💡 Environment Notes

- This project does not use a database.
- The backend caches flight offers temporarily using an in-memory map.
- The frontend and backend communicate via HTTP only.
- Uses Amadeus REST API (no SDK used) for flight data.

## 🔧 Possible Future Improvements

- Add unit and integration tests for the backend (JUnit + Mockito).
- Improve frontend form validation and error messages.
- Add CI/CD pipeline using GitHub Actions.
- Add support for multi-city search.
