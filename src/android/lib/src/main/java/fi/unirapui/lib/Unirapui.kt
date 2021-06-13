package fi.unirapui.lib

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import android.net.http.SslError
import android.util.Base64
import android.webkit.*
import androidx.annotation.RawRes
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

class Unirapui {
    companion object {
        init {
            System.loadLibrary("unirapui")
        }

        @JvmStatic
        private external fun start(index: String, port: Int)

        @JvmStatic
        private external fun echo(data: String?): String

        private class JavascriptInterface {
            @android.webkit.JavascriptInterface
            fun echo(data: String): String {
                return Companion.echo(data)
            }
        }

        @SuppressLint("SetJavaScriptEnabled")
        fun create(applicationContext: Context, port: UShort): WebView {
            val assetManager = applicationContext.assets
            val indexInputStream = assetManager.open("index.html")
            val index = String(indexInputStream.readBytes(), Charsets.UTF_8)
            indexInputStream.close()

            start(index, port.toInt())

            WebView.setWebContentsDebuggingEnabled(BuildConfig.DEBUG)
            val webView = WebView(applicationContext)
            webView.settings.javaScriptEnabled = true
            webView.addJavascriptInterface(JavascriptInterface(), "native")

            val caCertificate = getCertificate(applicationContext, R.raw.ca_certificate)
            val clientKey = getClientKey(applicationContext)
            val clientCertificate = getCertificate(applicationContext, R.raw.client_certificate)

            webView.webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    val (local, remote) = webView.createWebMessageChannel()
                    local.setWebMessageCallback(object : WebMessagePort.WebMessageCallback() {
                        override fun onMessage(port: WebMessagePort?, message: WebMessage?) {
                            local.postMessage(WebMessage(echo(message?.data)))
                        }
                    })
                    webView.postWebMessage(WebMessage(null, arrayOf(remote)), Uri.parse("https://127.0.0.1:$port"))
                }

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

            webView.loadUrl("https://127.0.0.1:$port")

            return webView
        }
    }
}