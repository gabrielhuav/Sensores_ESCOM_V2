package ovh.gabrielhuav.sensores_escom_v2

import android.app.AlertDialog
import android.content.Context
import android.view.LayoutInflater
import android.widget.EditText
import android.widget.Toast

class CoordinateInputDialog(
    private val context: Context,
    private val mapWidth: Int,
    private val mapHeight: Int,
    private val onCoordinatesSelected: (Int, Int) -> Unit
) {
    fun show() {
        // Inflate el layout del diálogo
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_coordinates, null)
        val editX = dialogView.findViewById<EditText>(R.id.editX)
        val editY = dialogView.findViewById<EditText>(R.id.editY)

        // Configura el diálogo
        AlertDialog.Builder(context)
            .setTitle("Ingrese las coordenadas")
            .setView(dialogView)
            .setPositiveButton("Aceptar") { _, _ ->
                val x = editX.text.toString().toIntOrNull()
                val y = editY.text.toString().toIntOrNull()

                if (x == null || y == null || x !in 0 until mapWidth || y !in 0 until mapHeight) {
                    Toast.makeText(context, "Coordenadas inválidas", Toast.LENGTH_SHORT).show()
                } else {
                    onCoordinatesSelected(x, y) // Envía las coordenadas seleccionadas
                }
            }
            .setNegativeButton("Cancelar", null)
            .create()
            .show()
    }
}
