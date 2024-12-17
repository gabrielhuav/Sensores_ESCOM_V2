package ovh.gabrielhuav.sensores_escom_v2.data.map.Game

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class GameViewModel : ViewModel() {
    // Tamaño dinámico del mapa
    private var mapWidth = 10 // EJE X Cambiar a un tamaño más grande según sea necesario
    private var mapHeight = 10 // EJE Y Cambiar a un tamaño más grande según sea necesario

    // Jugador inicializado en el centro del mapa
    private val player = Player(mapWidth / 2, mapHeight / 2, mapWidth, mapHeight)

    // Estado del jugador observable
    private val _playerPosition = MutableLiveData<Pair<Int, Int>>().apply {
        value = Pair(player.x, player.y)
    }
    val playerPosition: LiveData<Pair<Int, Int>> = _playerPosition

    // Movimiento del jugador
    fun moveNorth() {
        player.moveNorth()
        updatePlayerPosition()
    }

    fun moveSouth() {
        player.moveSouth()
        updatePlayerPosition()
    }

    fun moveEast() {
        player.moveEast()
        updatePlayerPosition()
    }

    fun moveWest() {
        player.moveWest()
        updatePlayerPosition()
    }

    private fun updatePlayerPosition() {
        _playerPosition.value = Pair(player.x, player.y)
    }

    // Método para actualizar dinámicamente el tamaño del mapa
    fun setMapSize(newWidth: Int, newHeight: Int) {
        mapWidth = newWidth
        mapHeight = newHeight
        player.updateMapBounds(mapWidth, mapHeight) // Actualizar límites del jugador
        updatePlayerPosition()
    }
}
