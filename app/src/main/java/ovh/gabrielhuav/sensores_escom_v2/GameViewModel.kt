package ovh.gabrielhuav.sensores_escom_v2

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class GameViewModel : ViewModel() {
    // Tamaño del mapa
    private val mapWidth = 10
    private val mapHeight = 10

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
}