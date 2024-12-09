package ovh.gabrielhuav.sensores_escom_v2

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class GameViewModel : ViewModel() {
    private var _playerPosition = MutableLiveData<Pair<Int, Int>?>()
    val playerPosition: LiveData<Pair<Int, Int>?> get() = _playerPosition

    private val maxX = 20 // Número máximo de casillas en el eje X
    private val maxY = 20 // Número máximo de casillas en el eje Y
    private var position = Pair(0, 0) // Posición inicial del jugador

    fun moveNorth() {
        val newY = (position.second - 1).coerceIn(0, maxY - 1)
        position = Pair(position.first, newY)
        _playerPosition.value = position
    }

    fun moveSouth() {
        val newY = (position.second + 1).coerceIn(0, maxY - 1)
        position = Pair(position.first, newY)
        _playerPosition.value = position
    }

    fun moveEast() {
        val newX = (position.first + 1).coerceIn(0, maxX - 1)
        position = Pair(newX, position.second)
        _playerPosition.value = position
    }

    fun moveWest() {
        val newX = (position.first - 1).coerceIn(0, maxX - 1)
        position = Pair(newX, position.second)
        _playerPosition.value = position
    }
}
