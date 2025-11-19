package ovh.gabrielhuav.sensores_escom_v2.presentation.locations.outdoor

import android.annotation.SuppressLint
import android.os.Bundle
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import ovh.gabrielhuav.sensores_escom_v2.R
import android.util.Log

class EstacionesMetro : AppCompatActivity() {

    private lateinit var webView: WebView
    private lateinit var backButton: Button

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_estaciones_metro)

        // Obtener coordenadas del intent
        val latitude = intent.getDoubleExtra("LATITUDE", 0.0)
        val longitude = intent.getDoubleExtra("LONGITUDE", 0.0)

        initializeViews()

        if (latitude != 0.0 && longitude != 0.0) {
            // Usar coordenadas directas
            setupWebViewWithCoordinates(latitude, longitude)
        } else {
            val searchQuery = intent.getStringExtra("SEARCH_QUERY") ?: "Metro CDMX"
            setupWebViewWithSearch(searchQuery)
        }
    }

    private fun initializeViews() {
        webView = findViewById(R.id.webView)
        backButton = findViewById(R.id.btnSearch)

        backButton.setOnClickListener {
            finish()
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebViewWithSearch(searchQuery: String) {
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            setSupportZoom(true)
            builtInZoomControls = true
            displayZoomControls = false
        }

        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                return false
            }
        }

        performSearch(searchQuery)
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebViewWithCoordinates(latitude: Double, longitude: Double) {
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            setSupportZoom(true)
            builtInZoomControls = true
            displayZoomControls = false
        }

        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                return false
            }
        }

        loadCoordinates(latitude, longitude)
    }

    private fun loadCoordinates(latitude: Double, longitude: Double) {
        try {
            // URL directa con coordenadas
            val url = "https://www.openstreetmap.org/#map=17/$latitude/$longitude"
            webView.loadUrl(url)
            Log.d("EstacionesMetro", "Cargando coordenadas: $latitude, $longitude")
        } catch (e: Exception) {
            Log.e("EstacionesMetro", "Error con coordenadas: ${e.message}")
            // Búsqueda genérica si fallan las coordenadas
            setupWebViewWithSearch("Metro CDMX")
        }
    }

    private fun performSearch(query: String) {
        try {
            val encodedQuery = java.net.URLEncoder.encode(query, "UTF-8")
            val url = "https://www.openstreetmap.org/search?query=$encodedQuery"
            webView.loadUrl(url)
            Log.d("EstacionesMetro", "Buscando: $query")
        } catch (e: Exception) {
            Log.e("EstacionesMetro", "Error en la búsqueda: ${e.message}")
        }
    }

    override fun onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack()
        } else {
            super.onBackPressed()
        }
    }
}