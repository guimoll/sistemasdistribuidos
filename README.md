# ⏰ Berkeley Clock Synchronization – Kotlin Client-Server App

This project implements the **Berkeley Algorithm** for synchronizing time between a central **Server** and multiple **Users** using a client-server architecture written in Kotlin.

---

## 🖥️ Components

- **Server**  
  Runs in IntelliJ and handles time synchronization logic. Listens for user connections and coordinates the Berkeley algorithm.

- **User**  
  Run from the terminal. Connects to the server and automatically responds to synchronization requests.

---

## 🛠️ Requirements

- Kotlin project built with IntelliJ
- Java installed and accessible via the `java` command
- The compiled `.class` files from the project
- The Kotlin standard library JAR (usually available via Maven or Kotlin installation)

---

## 🚀 Running the Server (in IntelliJ)

1. Open the project in **IntelliJ IDEA**.
2. Build the project: `Build > Build Project` (or `Ctrl + F9`).
3. Run the `Server.kt` file (right-click > Run or click the green play button).


---

## 🧑‍💻 Running a User (from the Terminal)

### Step 1: Locate Required Paths

To run a user client from the terminal, you need:

- **Your compiled Kotlin class output directory** (usually something like:  
  `path/to/project/out/production/your-project-name`)
- **The Kotlin standard library JAR**. You can find it:
  - In your local Maven repository (e.g.,  
    `~/.m2/repository/org/jetbrains/kotlin/kotlin-stdlib/X.Y.Z/kotlin-stdlib-X.Y.Z.jar`)
  - Or inside your Kotlin installation, depending on setup.

> On Windows, replace `/` with `\` and use quotes if paths contain spaces.

### Step 2: Run the User

Use this template to launch a user from the terminal:

java -cp "<path-to-compiled-classes>;<path-to-kotlin-stdlib-jar>" UserKt

WINDOWS EXAMPLE: java -cp "C:\Users\you\Documents\BerkeleyApp\out\production\BerkeleyApp;C:\Users\you\.m2\repository\org\jetbrains\kotlin\kotlin-stdlib\2.1.10\kotlin-stdlib-2.1.10.jar" UserKt

LINUX EXAMPLE: java -cp "/home/you/Projects/BerkeleyApp/out/production/BerkeleyApp:/home/you/.m2/repository/org/jetbrains/kotlin/kotlin-stdlib/2.1.10/kotlin-stdlib-2.1.10.jar" UserKt

### Step 3: Input Prompt

When the user client starts, you will be prompted to enter:

- Your **username**
- Your **initial time** (in `HH:mm` format)

---

## ✅ Example Workflow

1. Start the server in IntelliJ.
2. Run 3–4 users in separate terminals using the `java -cp` command.
3. Once all users are connected, type `sync` in the server terminal
4. Type `time` in any terminal (or the server console) to view the current synchronized time report.

---

## 💬 User Commands

- `sync`: Starts the Berkeley algorithm. Can only be used in the server terminal
- `time`: Displays the current time report for the server and all users.
- Anything else typed will be treated as a chat message and broadcasted to all users.

---

## ⏰ Berkeley Algorithm Overview

The Berkeley algorithm synchronizes the clocks of distributed systems using the following steps:

1. **Server Initiates Sync**: The server sends its current time to all connected users.
2. **Users Respond**: Each user calculates the offset (difference in minutes) between their local time and the server's time, and sends it back to the server.
3. **Server Computes Average Offset**:
    - It includes its own time as offset `0`.
    - Calculates the average offset of all nodes.
4. **Adjustments Sent**:
    - The server adjusts its own time by the average offset.
    - Each user receives an adjustment equal to `(average offset - their own offset)` and updates their time accordingly.
5. **Time Sync Complete**: All machines now share the same synchronized time.

 
