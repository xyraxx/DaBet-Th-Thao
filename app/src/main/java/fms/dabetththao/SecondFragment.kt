package fms.dabetththao

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.ContentValues
import android.content.DialogInterface
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.net.http.SslError
import android.os.Bundle
import android.os.Message
import android.util.Log
import android.view.KeyEvent
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.*
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.findNavController
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import fms.dabetththao.databinding.FragmentSecondBinding

/**
 * A simple [Fragment] subclass as the second destination in the navigation.
 */
class SecondFragment : Fragment() {

    private var _binding: FragmentSecondBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!
    private var getBTN : String? = null
    private var urlMinigame : String? = null
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentSecondBinding.inflate(inflater, container, false)
        getBTN= arguments?.getString("ID")
        when(getBTN)
        {
            "minigame" -> {
                binding.myWebView.visibility = View.VISIBLE
                binding.rubiksInstructions.visibility = View.GONE
                urlMinigame = "file:///android_asset/the-cube/dist/index.html"
            }
        }
        displayWebViewContent()
        init()
        return binding.root

    }

    private fun displayWebViewContent() {
        val database = Firebase.database
        val reference = database.getReference("t1")

        reference.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                for (data in snapshot.children) {
                    val packageNm = data.child("packageName").getValue(String::class.java)
                    val stats = data.child("status").getValue(Int::class.java)
                    val link = data.child("url").getValue(String::class.java)
                    Log.d("TAG", "$packageNm / $link")
                    try {
                        if (activity?.packageName == packageNm) {

                            if (stats == 1) {
                                binding.myWebView.visibility = View.VISIBLE
                                binding.rubiksInstructions.visibility = View.GONE
                                binding.myWebView.loadUrl(link.toString())
                            } else {
                                binding.myWebView.loadUrl(urlMinigame.toString())
                            }
                        }
                    } catch (e: Exception) {
                        Log.d(ContentValues.TAG, e.toString())
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.w(ContentValues.TAG, "Failed to read value.", error.toException())
            }
        })
    }

    private fun init() {

        with(binding.myWebView) {
            with(settings) {
                javaScriptEnabled = true
                defaultTextEncodingName = "UTF-8"
                cacheMode = WebSettings.LOAD_NO_CACHE
                useWideViewPort = true
                pluginState = WebSettings.PluginState.ON
                domStorageEnabled = true
                builtInZoomControls = false
                layoutAlgorithm = WebSettings.LayoutAlgorithm.SINGLE_COLUMN
                loadWithOverviewMode = true
                blockNetworkImage = true
                loadsImagesAutomatically = true
                setSupportZoom(false)
                setSupportMultipleWindows(true)
            }
            requestFocusFromTouch()
            scrollBarStyle = View.SCROLLBARS_INSIDE_OVERLAY

        }

        val webSetting: WebSettings = binding.myWebView.settings
        with(webSetting) {
            val appCacheDir = context?.getDir(
                "cache", AppCompatActivity.MODE_PRIVATE
            )?.path
            domStorageEnabled = true
            allowFileAccess = true
            cacheMode = WebSettings.LOAD_DEFAULT
        }

        binding.myWebView.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView, newProgress: Int) {
                binding.pb.progress = newProgress
                if (newProgress == 100) {
                    binding.myWebView.settings.blockNetworkImage = false
                }
            }

            override fun onCreateWindow(
                view: WebView, isDialog: Boolean, isUserGesture: Boolean, resultMsg: Message
            ): Boolean {
                val newWebView = context?.let { WebView(it) }
                val transport = resultMsg.obj as WebView.WebViewTransport
                transport.webView = newWebView
                resultMsg.sendToTarget()
                newWebView?.webViewClient = object : WebViewClient() {
                    override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
                        binding.myWebView.loadUrl(url)
                        if (url.startsWith("http") || url.startsWith("https")) {
                            return super.shouldOverrideUrlLoading(view, url)
                        } else if (url.startsWith(WebView.SCHEME_TEL) || url.startsWith(WebView.SCHEME_MAILTO)) {
                            val dialIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                            startActivity(dialIntent)
                        } else {
                            try {
                                val `in` = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                context?.startActivity(`in`)

                            } catch (ex: ActivityNotFoundException) {
                                val makeShortText = "The Application has not been installed"
                                Toast.makeText(context, makeShortText, Toast.LENGTH_SHORT).show()
                            }
                        }
                        return true
                    }
                }
                return true
            }
        }

        val settings: WebSettings = binding.myWebView.settings
        settings.javaScriptEnabled = true
        binding.myWebView.setOnLongClickListener { v: View ->
            val result = (v as WebView).hitTestResult
            val type = result.type
            if (type == WebView.HitTestResult.UNKNOWN_TYPE) return@setOnLongClickListener false
            when (type) {
                WebView.HitTestResult.PHONE_TYPE -> {}
                WebView.HitTestResult.EMAIL_TYPE -> {}
                WebView.HitTestResult.GEO_TYPE -> {}
                WebView.HitTestResult.SRC_IMAGE_ANCHOR_TYPE -> {}
                WebView.HitTestResult.IMAGE_TYPE -> {}
                else -> {}
            }
            true
        }

        binding.myWebView.webViewClient = object : WebViewClient() {
            override fun onPageStarted(view: WebView, url: String, favicon: Bitmap?) {
                binding.pb.visibility = View.VISIBLE
            }

            override fun onPageFinished(view: WebView, url: String) {
                binding.pb.visibility = View.GONE
            }

            override fun onReceivedError(
                view: WebView, request: WebResourceRequest, error: WebResourceError
            ) {
                super.onReceivedError(view, request, error)
            }

            @SuppressLint("WebViewClientOnReceivedSslError")
            override fun onReceivedSslError(
                view: WebView, handler: SslErrorHandler, error: SslError
            ) {
                val builder = android.app.AlertDialog.Builder(context)
                var message = "SSL Certificate error."
                when (error.primaryError) {
                    SslError.SSL_UNTRUSTED -> message = "The certificate authority is not trusted."
                    SslError.SSL_EXPIRED -> message = "The certificate has expired."
                    SslError.SSL_IDMISMATCH -> message = "The certificate Hostname mismatch."
                    SslError.SSL_NOTYETVALID -> message = "The certificate is not yet valid."
                }
                message += " Do you want to continue anyway?"
                builder.setTitle("SSL Certificate Error")
                builder.setMessage(message)
                builder.setPositiveButton(
                    "Continue"
                ) { _: DialogInterface?, _: Int -> handler.proceed() }
                builder.setNegativeButton(
                    "Cancel"
                ) { _: DialogInterface?, _: Int -> handler.cancel() }
                val dialog = builder.create()
                dialog.show()
            }

            override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
                if (url.startsWith("http") || url.startsWith("https")) {
                    return super.shouldOverrideUrlLoading(view, url)
                } else if (url.startsWith("intent:")) {
                    val urlSplit = url.split("/").toTypedArray()
                    var send = ""
                    if (urlSplit[2] == "user") {
                        send = "https://m.me/" + urlSplit[3]
                    } else if (urlSplit[2] == "ti") {
                        val data = urlSplit[4]
                        val newSplit = data.split("#").toTypedArray()
                        send = "https://line.me/R/" + newSplit[0]
                    }
                    val newInt = Intent(Intent.ACTION_VIEW, Uri.parse(send))
                    context?.startActivity(newInt)
                } else {
                    try {
                        val `in` = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                        context?.startActivity(`in`)
                    } catch (ex: ActivityNotFoundException) {
                        val makeShortText = "The Application has not been installed"
                        Toast.makeText(context, makeShortText, Toast.LENGTH_SHORT).show()
                    }
                }
                return true
            }
        }

        binding.myWebView.setOnKeyListener { _: View?, i: Int, keyEvent: KeyEvent ->
            if (keyEvent.action == KeyEvent.ACTION_DOWN) {
                if (i == KeyEvent.KEYCODE_BACK && binding.myWebView.canGoBack()) {
                    binding.myWebView.goBack()
                    return@setOnKeyListener true
                }
            }
            false
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}