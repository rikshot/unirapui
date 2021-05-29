package fi.unirapui.example

import android.content.Context
import android.net.http.SslError
import android.os.Bundle
import android.util.Base64
import android.webkit.ClientCertRequest
import android.webkit.SslErrorHandler
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.annotation.RawRes
import androidx.appcompat.app.AppCompatActivity
import fi.unirapui.Unirapui
import java.io.InputStream
import java.security.KeyFactory
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import java.security.interfaces.RSAPrivateKey
import java.security.spec.PKCS8EncodedKeySpec

fun getCertificate(applicationContext: Context, @RawRes id: Int): X509Certificate? {
    val certificateFactory: CertificateFactory = CertificateFactory.getInstance("X.509")
    val inputStream: InputStream =
        applicationContext.resources.openRawResource(id)
    val clientCertificate = certificateFactory.generateCertificate(
        inputStream
    ) as X509Certificate?
    inputStream.close()
    return clientCertificate
}

fun getClientKey(applicationContext: Context): RSAPrivateKey {
    val keyFactory: KeyFactory = KeyFactory.getInstance("RSA")
    val inputStream = applicationContext.resources.openRawResource(R.raw.client_key)
    val byteArray = inputStream.readBytes()
    inputStream.close()
    var privateKey = byteArray.decodeToString()
    privateKey = privateKey.replace("-----BEGIN RSA PRIVATE KEY-----", "")
    privateKey = privateKey.replace("-----END RSA PRIVATE KEY-----", "")
    privateKey = privateKey.replace("\n", "")
    val keySpec = PKCS8EncodedKeySpec(Base64.decode(privateKey.toByteArray(), Base64.DEFAULT))
    return keyFactory.generatePrivate(keySpec) as RSAPrivateKey
}

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        Unirapui.start(8000)

        val webView: WebView = findViewById(R.id.webview)

        val caCertificate = getCertificate(applicationContext, R.raw.ca_certificate)
        val clientKey = getClientKey(applicationContext)
        val clientCertificate = getCertificate(applicationContext, R.raw.client_certificate)

        webView.webViewClient = object : WebViewClient() {
            override fun onReceivedSslError(
                view: WebView?,
                handler: SslErrorHandler?,
                error: SslError?
            ) {
                val serverCertificate = error?.certificate?.x509Certificate
                try {
                    serverCertificate?.checkValidity()
                    serverCertificate?.verify(caCertificate?.publicKey)
                    handler?.proceed()
                } catch (e: Exception) {
                    super.onReceivedSslError(view, handler, error)
                }
            }

            override fun onReceivedClientCertRequest(view: WebView?, request: ClientCertRequest?) {
                request?.proceed(clientKey, arrayOf(clientCertificate))
            }
        }

        webView.loadUrl("https://127.0.0.1:8000/hello/world")
    }

}