# Online Server - WebSocket & Attendance System

This server handles WebSocket connections for real-time player tracking and includes an attendance system for managing class attendance.

## Features

- **WebSocket Server**: Real-time player position tracking
- **Zombie Game**: Mini-game with zombie AI
- **Attendance System**: Record and query student attendance with duplicate prevention

## Prerequisites

- Node.js (v14 or higher)
- Docker and Docker Compose
- npm or yarn

## Setup Instructions

### 1. Install Dependencies

```powershell
npm install
```

### 2. Start the Database

Start the PostgreSQL database using Docker Compose:

```powershell
docker-compose up -d
```

This will start a PostgreSQL container on port 5432.

### 3. Initialize Prisma

Generate the Prisma Client and run migrations:

```powershell
# Generate Prisma Client
npx prisma generate

# Create and run the initial migration
npx prisma migrate dev --name init
```

### 4. Start the Server

```powershell
node server.js
```

The server will start on `http://localhost:3000`

## Attendance API

### Register Attendance

**Endpoint:** `POST /attendance`

**Request Body:**
```json
{
  "phoneID": "unique-phone-identifier",
  "fullName": "John Doe",
  "group": "3CM1"
}
```

**Success Response (201):**
```json
{
  "success": true,
  "message": "Attendance registered successfully",
  "data": {
    "id": 1,
    "phoneID": "unique-phone-identifier",
    "attendanceTime": "2025-10-29T10:30:00.000Z",
    "fullName": "John Doe",
    "group": "3CM1"
  }
}
```

**Error Response - Already Attended (409):**
```json
{
  "success": false,
  "error": "Attendance already registered for this phoneID today"
}
```

**Error Response - Missing Fields (400):**
```json
{
  "success": false,
  "error": "Missing required fields: phoneID, fullName, and group are required"
}
```

### Get Attendance List

**Endpoint:** `GET /attendance/:date/:group`

**Parameters:**
- `date`: Date in YYYY-MM-DD format (e.g., 2025-10-29)
- `group`: Group name (e.g., 3CM1)

**Example:**
```
GET /attendance/2025-10-29/3CM1
```

**Success Response (200):**
```json
{
  "success": true,
  "date": "2025-10-29",
  "group": "3CM1",
  "count": 2,
  "attendees": [
    {
      "phoneID": "phone-001",
      "attendanceTime": "2025-10-29T10:30:00.000Z",
      "fullName": "John Doe",
      "group": "3CM1"
    },
    {
      "phoneID": "phone-002",
      "attendanceTime": "2025-10-29T10:35:00.000Z",
      "fullName": "Jane Smith",
      "group": "3CM1"
    }
  ]
}
```

## Testing the Attendance API

### Using PowerShell

**Register Attendance:**
```powershell
$body = @{
    phoneID = "test-phone-001"
    fullName = "John Doe"
    group = "3CM1"
} | ConvertTo-Json

Invoke-RestMethod -Uri "http://localhost:3000/attendance" -Method POST -Body $body -ContentType "application/json"
```

**Get Attendance List:**
```powershell
Invoke-RestMethod -Uri "http://localhost:3000/attendance/2025-10-29/3CM1" -Method GET
```

### Using cURL (Git Bash or WSL)

**Register Attendance:**
```bash
curl -X POST http://localhost:3000/attendance \
  -H "Content-Type: application/json" \
  -d '{"phoneID":"test-phone-001","fullName":"John Doe","group":"3CM1"}'
```

**Get Attendance List:**
```bash
curl http://localhost:3000/attendance/2025-10-29/3CM1
```

## Database Management

### View Database with Prisma Studio

```powershell
npx prisma studio
```

This opens a web interface at `http://localhost:5555` to view and edit database records.

### Reset Database

```powershell
npx prisma migrate reset
```

### Stop Database

```powershell
docker-compose down
```

To also remove the stored data:
```powershell
docker-compose down -v
```

## Attendance Business Rules

1. **Duplicate Prevention**: A user with the same `phoneID` cannot register attendance twice on the same day
2. **Automatic Timestamp**: The `attendanceTime` is automatically set to the current date and time
3. **Group Filtering**: Attendance can be queried by date and group

## Environment Variables

The `.env` file contains:
```
DATABASE_URL="postgresql://attendance_user:attendance_pass@localhost:5432/attendance_db"
PORT=3000
```

## Project Structure

```
Online-Server/
├── prisma/
│   └── schema.prisma          # Database schema
├── server.js                  # Main server file
├── zombieController.js        # Zombie game logic
├── collisionMatrices.js       # Collision detection
├── docker-compose.yml         # Database container config
├── package.json               # Dependencies
├── .env                       # Environment variables
└── README.md                  # This file
```

## Troubleshooting

### Database Connection Issues

If you see connection errors:
1. Ensure Docker is running
2. Check if the database container is up: `docker ps`
3. Verify the connection string in `.env` matches your Docker setup

### Port Already in Use

If port 3000 or 5432 is already in use:
- Change the server port in `.env`
- Change the database port mapping in `docker-compose.yml`

### Prisma Client Not Found

Run:
```powershell
npx prisma generate
```

## WebSocket Features (Existing)

The server also includes WebSocket functionality for:
- Player position tracking
- Zombie mini-game
- Multi-map support

See the original documentation for WebSocket usage.
