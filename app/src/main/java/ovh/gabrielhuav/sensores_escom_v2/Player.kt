package ovh.gabrielhuav.sensores_escom_v2

data class Player(
    var x: Int = 0,
    var y: Int = 0,
    val mapWidth: Int,
    val mapHeight: Int
) {
    init {
        // Coloca al jugador en una posición aleatoria si no se especifica
        //if (x == 0 && y == 0) {
            x = (0 until mapWidth).random()
            y = (0 until mapHeight).random()
        //}
    }

    // Movimiento hacia el norte
    fun moveNorth() {
        if (y > 0) y--
    }

    // Movimiento hacia el sur
    fun moveSouth() {
        if (y < mapHeight - 1) y++
    }

    // Movimiento hacia el este
    fun moveEast() {
        if (x < mapWidth - 1) x++
    }

    // Movimiento hacia el oeste
    fun moveWest() {
        if (x > 0) x--
    }
}
