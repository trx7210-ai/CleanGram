package com.cleangram.app

import android.net.Uri
import android.os.Bundle
import android.webkit.*
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var web: WebView
    private var filePathCallback: ValueCallback<Array<Uri>>? = null

    private val picker = registerForActivityResult(
        ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        filePathCallback?.onReceiveValue(uris.toTypedArray())
        filePathCallback = null
    }

    private val adScript = """
    (function(){
      const LABELS=['Sponsored','Paid partnership'];
      function hideAds(){
        const labels=[...document.querySelectorAll('span')]
          .filter(s=>LABELS.includes(s.textContent.trim()));
        labels.forEach(label=>{
          let el=label.closest('article');
          if(!el){
            el=label;
            while(el.parentElement && el.offsetHeight<window.innerHeight*0.8){el=el.parentElement;}
          }
          if(el && el!==document.body) el.style.display='none';
        });
        if(location.pathname.startsWith('/stories/') && labels.length){
          const next=document.querySelector('[aria-label="Next"]');
          if(next) next.click();
        }
      }
      new MutationObserver(hideAds).observe(document.body,{childList:true,subtree:true});
      hideAds();
    })();
    """.trimIndent()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        web = WebView(this)
        setContentView(web)

        CookieManager.getInstance().setAcceptCookie(true)

        web.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            mediaPlaybackRequiresUserGesture = false
            userAgentString = "Mozilla/5.0 (Linux; Android 13; Pixel 7) AppleWebKit/537.36 " +
                "(KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
        }

        web.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView, url: String) {
                view.evaluateJavascript(adScript, null)
            }
        }

        web.webChromeClient = object : WebChromeClient() {
            override fun onShowFileChooser(
                w: WebView, cb: ValueCallback<Array<Uri>>, params: FileChooserParams
            ): Boolean {
                filePathCallback?.onReceiveValue(null)
                filePathCallback = cb
                picker.launch("*/*")
                return true
            }
        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (web.canGoBack()) web.goBack() else finish()
            }
        })

        web.loadUrl("https://www.instagram.com/")
    }
}
