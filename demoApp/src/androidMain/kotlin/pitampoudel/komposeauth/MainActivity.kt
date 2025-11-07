package pitampoudel.komposeauth

import BuildKonfig
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.chuckerteam.chucker.api.ChuckerInterceptor
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import pitampoudel.komposeauth.core.data.installKomposeAuth
import pitampoudel.komposeauth.core.di.initializeKomposeAuth

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val httpClient = HttpClient(OkHttp) {
            engine {
                addInterceptor(ChuckerInterceptor(this@MainActivity))
            }
            installKomposeAuth(
                this@MainActivity,
                authServerUrl = BuildKonfig.LOCAL_SERVER_URL,
                resourceServerUrls = listOf(
                    "https://your-resource-server"
                )
            )
        }
        initializeKomposeAuth(
            httpClient = httpClient
        )
        setContent {
            App()
        }
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    App()
}