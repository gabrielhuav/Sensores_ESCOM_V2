package ovh.gabrielhuav.sensores_escom_v2.data.map.Game

class Player(
    var x: Int,
    var y: Int,
    private var mapWidth: Int,
    private var mapHeight: Int
) {
    fun moveNorth() {
        if (y > 0) y--
    }

    fun moveSouth() {
        if (y < mapHeight - 1) y++
    }

    fun moveEast() {
        if (x < mapWidth - 1) x++
    }

    fun moveWest() {
        if (x > 0) x--
    }

    // Actualizar los límites del mapa
    fun updateMapBounds(newWidth: Int, newHeight: Int) {
        mapWidth = newWidth
        mapHeight = newHeight
    }
}
