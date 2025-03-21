package ovh.gabrielhuav.sensores_escom_v2.presentation.components.ipn.zacatenco.escom.buildingNumber3.pacman

import android.os.Handler
import android.os.Looper
import android.util.Log
import kotlin.math.abs
import kotlin.math.sqrt

enum class PacmanGameStatus {
    PLAYING,
    PAUSED,
    WIN,
    LOSE,
    UPDATE
}

class PacmanController(
    private val onGameStateChanged: (PacmanGameStatus) -> Unit,
    private val onEntityPositionChanged: (String, Pair<Int, Int>, String) -> Unit
) {
    // Constants for directions (same as in the JS version)
    companion object {
        const val DIRECTION_RIGHT = 4
        const val DIRECTION_UP = 3
        const val DIRECTION_LEFT = 2
        const val DIRECTION_BOTTOM = 1

        private const val TAG = "PacmanController"
    }

    // Game variables
    private var isGameActive = false
    private var isPaused = false
    private var score = 0
    private var lives = 3

    // Game map - 1 is wall, 2 is food, 3 is empty (food was eaten), 0 is empty
    private var gameMap = arrayOf(
        arrayOf(1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1),
        arrayOf(1, 2, 2, 2, 2, 2, 2, 2, 2, 2, 1, 2, 2, 2, 2, 2, 2, 2, 2, 2, 1),
        arrayOf(1, 2, 1, 1, 1, 2, 1, 1, 1, 2, 1, 2, 1, 1, 1, 2, 1, 1, 1, 2, 1),
        arrayOf(1, 2, 1, 1, 1, 2, 1, 1, 1, 2, 1, 2, 1, 1, 1, 2, 1, 1, 1, 2, 1),
        arrayOf(1, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 1),
        arrayOf(1, 2, 1, 1, 1, 2, 1, 2, 1, 1, 1, 1, 1, 2, 1, 2, 1, 1, 1, 2, 1),
        arrayOf(1, 2, 2, 2, 2, 2, 1, 2, 2, 2, 1, 2, 2, 2, 1, 2, 2, 2, 2, 2, 1),
        arrayOf(1, 1, 1, 1, 1, 2, 1, 1, 1, 2, 1, 2, 1, 1, 1, 2, 1, 1, 1, 1, 1),
        arrayOf(0, 0, 0, 0, 1, 2, 1, 2, 2, 2, 2, 2, 2, 2, 1, 2, 1, 0, 0, 0, 0),
        arrayOf(1, 1, 1, 1, 1, 2, 1, 2, 1, 1, 0, 1, 1, 2, 1, 2, 1, 1, 1, 1, 1),
        arrayOf(0, 0, 0, 0, 0, 2, 2, 2, 1, 0, 0, 0, 1, 2, 2, 2, 0, 0, 0, 0, 0),
        arrayOf(1, 1, 1, 1, 1, 2, 1, 2, 1, 1, 1, 1, 1, 2, 1, 2, 1, 1, 1, 1, 1),
        arrayOf(0, 0, 0, 0, 1, 2, 1, 2, 2, 2, 2, 2, 2, 2, 1, 2, 1, 0, 0, 0, 0),
        arrayOf(1, 1, 1, 1, 1, 2, 2, 2, 1, 1, 1, 1, 1, 2, 2, 2, 1, 1, 1, 1, 1),
        arrayOf(1, 2, 2, 2, 2, 2, 2, 2, 2, 2, 1, 2, 2, 2, 2, 2, 2, 2, 2, 2, 1),
        arrayOf(1, 2, 1, 1, 1, 2, 1, 1, 1, 2, 1, 2, 1, 1, 1, 2, 1, 1, 1, 2, 1),
        arrayOf(1, 2, 2, 2, 1, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 1, 2, 2, 2, 1),
        arrayOf(1, 1, 2, 2, 1, 2, 1, 2, 1, 1, 1, 1, 1, 2, 1, 2, 1, 2, 2, 1, 1),
        arrayOf(1, 2, 2, 2, 2, 2, 1, 2, 2, 2, 1, 2, 2, 2, 1, 2, 2, 2, 2, 2, 1),
        arrayOf(1, 2, 1, 1, 1, 1, 1, 1, 1, 2, 1, 2, 1, 1, 1, 1, 1, 1, 1, 2, 1),
        arrayOf(1, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 1),
        arrayOf(1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1),
    )

    private var pacmanPosition = Pair(10, 15) // Start position
    private var pacmanDirection = DIRECTION_RIGHT
    private var pacmanNextDirection = DIRECTION_RIGHT
    private var ghosts = mutableListOf<Ghost>()
    private var foods = mutableListOf<Pair<Int, Int>>()
    private val handler = Handler(Looper.getMainLooper())
    private val updateIntervalMs = 250 // Update interval in milliseconds
    private var gameLoopRunnable: Runnable? = null

    // Targets for ghosts when not chasing pacman
    private val randomTargets = listOf(
        Pair(1, 1),
        Pair(1, 20),
        Pair(19, 1),
        Pair(19, 20)
    )

    // Class to represent a ghost
    inner class Ghost(
        var position: Pair<Int, Int>,
        var direction: Int = DIRECTION_RIGHT,
        var speed: Int = 1,
        var color: String = "red",
        var range: Int = 5
    ) {
        var ghostId: String = ""  // Changed from 'id' to 'ghostId'
        var lastMove = System.currentTimeMillis()

        fun setId(id: String) {
            this.ghostId = id
        }

        fun isInRange(): Boolean {
            val xDistance = abs(pacmanPosition.first - position.first)
            val yDistance = abs(pacmanPosition.second - position.second)
            return sqrt((xDistance * xDistance + yDistance * yDistance).toDouble()) <= range
        }

        fun changeDirection() {
            // Simple AI for ghost movement
            if (isInRange()) {
                // Chase pacman
                val dx = pacmanPosition.first - position.first
                val dy = pacmanPosition.second - position.second

                if (abs(dx) > abs(dy)) {
                    direction = if (dx > 0) DIRECTION_RIGHT else DIRECTION_LEFT
                } else {
                    direction = if (dy > 0) DIRECTION_BOTTOM else DIRECTION_UP
                }
            } else {
                // Random movement
                direction = (1..4).random()
            }
        }

        fun move() {
            // Only move if enough time has passed (based on ghost speed)
            val currentTime = System.currentTimeMillis()
            if (currentTime - lastMove < 1000 / speed) {
                return
            }

            lastMove = currentTime

            // Try to change direction
            if (Math.random() < 0.2) { // 20% chance to reconsider direction
                changeDirection()
            }

            // Store old position for collision check
            val oldPosition = position

            // Move based on direction
            val newPosition = when (direction) {
                DIRECTION_RIGHT -> Pair(position.first + 1, position.second)
                DIRECTION_LEFT -> Pair(position.first - 1, position.second)
                DIRECTION_UP -> Pair(position.first, position.second - 1)
                DIRECTION_BOTTOM -> Pair(position.first, position.second + 1)
                else -> position
            }

            // Check if new position is valid (not a wall)
            if (isValidPosition(newPosition.first, newPosition.second)) {
                position = newPosition
                onEntityPositionChanged(ghostId, position, "ghost")
            } else {
                // Hit a wall, change direction
                direction = (1..4).random()
            }

            // Check for collision with pacman
            if (position.first == pacmanPosition.first && position.second == pacmanPosition.second) {
                handlePacmanGhostCollision()
            }
        }
    }

    // Main game methods
    fun startGame(initialScore: Int = 0, initialLives: Int = 3) {
        if (isGameActive) return

        // Initialize game variables
        isGameActive = true
        isPaused = false
        score = initialScore
        lives = initialLives
        pacmanPosition = Pair(10, 15)
        pacmanDirection = DIRECTION_RIGHT
        pacmanNextDirection = DIRECTION_RIGHT

        // Initialize food positions
        initializeFoods()

        // Initialize ghosts
        initializeGhosts()

        // Update all entities' positions
        updatePacmanPosition()
        updateGhostPositions()

        // Start game loop
        startGameLoop()

        // Notify game started
        onGameStateChanged(PacmanGameStatus.PLAYING)
    }

    fun stopGame() {
        if (!isGameActive) return

        isGameActive = false
        isPaused = false

        // Stop game loop
        gameLoopRunnable?.let { handler.removeCallbacks(it) }
        gameLoopRunnable = null
    }

    fun pauseGame() {
        if (!isGameActive || isPaused) return

        isPaused = true
        gameLoopRunnable?.let { handler.removeCallbacks(it) }
    }

    fun resumeGame() {
        if (!isGameActive || !isPaused) return

        isPaused = false
        startGameLoop()
    }

    fun setDirection(direction: Int) {
        if (!isGameActive) return

        pacmanNextDirection = direction
    }

    fun getScore(): Int = score

    fun getLives(): Int = lives

// In PacmanController.kt, make sure the game loop is working correctly
// Find the startGameLoop method and ensure it's implemented correctly:

    private fun startGameLoop() {
        // First clean up any existing game loop
        gameLoopRunnable?.let { handler.removeCallbacks(it) }
        gameLoopRunnable = null

        // Create a new game loop runnable
        val runnable = object : Runnable {
            override fun run() {
                if (isGameActive && !isPaused) {
                    try {
                        // Log to verify the game loop is running
                        Log.d(TAG, "Game loop running. Pacman at: $pacmanPosition")

                        // Run a game update cycle
                        update()

                        // Schedule the next update
                        handler.postDelayed(this, updateIntervalMs.toLong())
                    } catch (e: Exception) {
                        Log.e(TAG, "Error in game loop: ${e.message}")
                        e.printStackTrace()
                        // Try to keep the game running despite errors
                        handler.postDelayed(this, updateIntervalMs.toLong())
                    }
                } else {
                    Log.d(TAG, "Game loop stopped. Active: $isGameActive, Paused: $isPaused")
                }
            }
        }

        // Store and start the game loop
        gameLoopRunnable = runnable
        handler.post(runnable)

        Log.d(TAG, "Game loop started with interval: $updateIntervalMs ms")
    }

    private fun update() {
        // Update pacman
        updatePacmanDirection()
        movePacman()
        checkFoodCollision()

        // Update ghosts
        updateGhostPositions()

        // Check win condition
        checkWinCondition()

        // Notify state update
        onGameStateChanged(PacmanGameStatus.UPDATE)
    }

    private fun initializeFoods() {
        foods.clear()

        // Add foods based on the map (where value is 2)
        for (y in gameMap.indices) {
            for (x in gameMap[y].indices) {
                if (gameMap[y][x] == 2) {
                    foods.add(Pair(x, y))
                    // Update entity in view
                    onEntityPositionChanged("food_${x}_${y}", Pair(x, y), "food")
                }
            }
        }
    }

    private fun initializeGhosts() {
        ghosts.clear()

        // Create 4 ghosts with different colors and starting positions
        val ghostColors = listOf("red", "pink", "cyan", "orange")
        val startPositions = listOf(
            Pair(9, 10),
            Pair(10, 10),
            Pair(11, 10),
            Pair(10, 9)
        )

        for (i in 0 until 4) {
            val ghost = Ghost(
                position = startPositions[i],
                direction = (1..4).random(),
                speed = 2 + (i % 2), // Different speeds
                color = ghostColors[i],
                range = 5 + i // Different chase ranges
            )
            ghost.setId("ghost_$i")
            ghosts.add(ghost)

            // Update entity in view
            onEntityPositionChanged(ghost.ghostId, ghost.position, "ghost")
        }
    }

    private fun updatePacmanPosition() {
        // Notificar cambio de posición
        onEntityPositionChanged("pacman", pacmanPosition, "pacman")

        // También notificar la dirección actual para la animación correcta
        onEntityPositionChanged("pacman_direction", pacmanPosition, pacmanDirection.toString())

        // Añadir log para verificar
        Log.d(TAG, "Pacman en posición $pacmanPosition con dirección $pacmanDirection")
    }


    private fun updateGhostPositions() {
        ghosts.forEach { ghost ->
            ghost.move()
        }
    }

    private fun updatePacmanDirection() {
        // Try to change direction
        if (pacmanDirection != pacmanNextDirection) {
            val testPosition = getPositionAfterMove(pacmanPosition, pacmanNextDirection)

            // If the new direction is valid (not a wall), change direction
            if (isValidPosition(testPosition.first, testPosition.second)) {
                pacmanDirection = pacmanNextDirection
            }
        }
    }

    private fun movePacman() {
        // Get new position based on current direction
        val newPosition = getPositionAfterMove(pacmanPosition, pacmanDirection)

        // Check if the new position is valid
        if (isValidPosition(newPosition.first, newPosition.second)) {
            pacmanPosition = newPosition
            updatePacmanPosition()

            // Check for ghost collision
            for (ghost in ghosts) {
                if (ghost.position.first == pacmanPosition.first &&
                    ghost.position.second == pacmanPosition.second) {
                    handlePacmanGhostCollision()
                    break
                }
            }
        }
    }

    private fun getPositionAfterMove(position: Pair<Int, Int>, direction: Int): Pair<Int, Int> {
        return when (direction) {
            DIRECTION_RIGHT -> Pair(position.first + 1, position.second)
            DIRECTION_LEFT -> Pair(position.first - 1, position.second)
            DIRECTION_UP -> Pair(position.first, position.second - 1)
            DIRECTION_BOTTOM -> Pair(position.first, position.second + 1)
            else -> position
        }
    }

    private fun checkFoodCollision() {
        // Find food at pacman's position
        val foodIndex = foods.indexOfFirst {
            it.first == pacmanPosition.first && it.second == pacmanPosition.second
        }

        if (foodIndex != -1) {
            // Remove food
            val food = foods.removeAt(foodIndex)

            // Update map
            gameMap[food.second][food.first] = 3 // Mark as eaten

            // Update score
            score += 10

            // Update UI (remove food entity)
            onEntityPositionChanged("food_${food.first}_${food.second}", Pair(-1, -1), "remove")
        }
    }

    private fun handlePacmanGhostCollision() {
        lives--

        if (lives <= 0) {
            // Game over
            onGameStateChanged(PacmanGameStatus.LOSE)
            stopGame()
        } else {
            // Reset positions
            pacmanPosition = Pair(10, 15)
            pacmanDirection = DIRECTION_RIGHT
            pacmanNextDirection = DIRECTION_RIGHT

            // Reset ghost positions
            initializeGhosts()

            // Update UI
            updatePacmanPosition()
        }
    }

    private fun isValidPosition(x: Int, y: Int): Boolean {
        // Check if position is within bounds
        if (y < 0 || y >= gameMap.size || x < 0 || x >= gameMap[0].size) {
            return false
        }

        // Check if position is not a wall
        return gameMap[y][x] != 1
    }

    private fun checkWinCondition() {
        // Win if all foods are eaten
        if (foods.isEmpty()) {
            onGameStateChanged(PacmanGameStatus.WIN)
            stopGame()
        }
    }
}
