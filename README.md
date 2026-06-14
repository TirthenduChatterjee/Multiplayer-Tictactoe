# Online Tic Tac Toe

A dark modern Tic Tac Toe web app where one player creates a room, shares a room code, and both players make live moves online.

## Stack

- Backend: Java 17, Spring Boot, Spring WebSocket, Spring Data JPA
- Frontend: React, Vite
- Database: MySQL

## Run MySQL

Create a MySQL user/database or update the credentials in `backend/src/main/resources/application.properties`.

## Run Backend

```bash
cd backend
mvn spring-boot:run
```

The API runs on `http://localhost:8080`.

## Run Frontend

```bash
cd frontend
npm install
npm run dev
```

The app runs on `http://localhost:5173`.

## Gameplay

1. Enter a player name and create a room.
2. Share the six-character room code with a friend.
3. The friend enters their name and joins using the room code.
4. Moves sync through WebSocket messages at `/ws/rooms/{code}`.
