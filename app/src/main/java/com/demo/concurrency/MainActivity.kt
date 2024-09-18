package com.demo.concurrency

import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Button
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.demo.concurrency.databinding.ActivityMainBinding
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.schedulers.Schedulers
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

private var countdownThread: Thread? = null
private val handler = Handler(Looper.getMainLooper())
private var countdownDisposable: Disposable? = null


class MainActivity : AppCompatActivity() {


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }


        binding.btnThread.setOnClickListener {
            threadCountDownClick(this,binding.tvThread)
        }
        binding.btnHandler.setOnClickListener {
            handlerCountDownClick(binding.tvHandler)
        }
        binding.btnRxjava.setOnClickListener {
            rxJavaCountDownClick(binding.tvRxjava)
        }
        binding.btnCoroutine.setOnClickListener {
            coroutineCountDownClick(binding.tvCoroutine)
        }

    }


    override fun onDestroy() {
        super.onDestroy()
        // 結束 Activity 時，停止執行緒
        countdownThread?.interrupt()
        // 移除 Handler 的所有 Callback 與 Message
        handler.removeCallbacksAndMessages(null)
        // 取消 RxJava 的訂閱
        countdownDisposable?.dispose()
    }
}




fun threadCountDownClick(activity: AppCompatActivity, text: TextView) {
    // 啟動倒數 10 秒的執行緒
    countdownThread = Thread {
        for (i in 10 downTo 0) {
            // 在主執行緒更新 UI
            activity.runOnUiThread {
                text.text = "Thread倒數：$i 秒"
            }
            Thread.sleep(1000)  // Thread.sleep是一種耗時操作,會阻塞當前執行緒,這個執行緒在接下來的時間就會被占用
        }
        // 倒數結束時，更新 UI 顯示完成
        activity.runOnUiThread {
            text.text = "Thread:倒數完成！"
        }
    }
    // 開啟執行緒
    countdownThread?.start()
}

fun threadNoChangeUIThreadClick(text: TextView) {
    // 啟動倒數 10 秒的執行緒
    countdownThread = Thread {
        for (i in 10 downTo 0) {
            // 在主執行緒更新 UI
            text.text = "Thread倒數：$i 秒"
            Thread.sleep(1000)  // 等待一秒
        }
        // 倒數結束時，更新 UI 顯示完成
        text.text = "Thread:倒數完成！"
    }
    // 開啟執行緒
    countdownThread?.start()
}

fun handlerCountDownClick(textView: TextView) {
    var count = 10
    handler.post(object : Runnable {
        override fun run() {
            if (count >= 0) {
                textView.text = "Handler倒數：$count 秒"
                count--
                handler.postDelayed(this, 1000)
            } else {
                textView.text = "Handler:倒數完成！"
            }
        }
    })
}

fun rxJavaCountDownClick(textView: TextView) {
    countdownDisposable = Observable.intervalRange(0, 11, 0, 1, TimeUnit.SECONDS)
        .map { 10 - it }
        // 設定在 IO 執行緒訂閱
        .subscribeOn(Schedulers.io())
        // 設定在主執行緒觀察
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(
            { value -> textView.text = "rxJava倒數：$value 秒" },
            { error -> error.printStackTrace() },
            { textView.text = "rxJava:倒數完成！" }
        )
}


fun coroutineCountDownClick(textView: TextView) {
   val job = CoroutineScope(Dispatchers.IO).launch {
        // 倒數 10 秒
        for (i in 10 downTo 0) {
            // 更新 UI
            withContext(Dispatchers.Main) {
                textView.text = "coroutine倒數：$i 秒"
            }
            //自動跳回 IO 執行緒
            delay(1000L)
            // 與Thread跟Handler不同 ,這裡沒有被阻塞,而是被暫停了,等suspend fun執行完後會自動跳回這裡繼續執行
        }
        // 倒數完成後更新 UI
        textView.text = "coroutine:倒數完成！"
    }

    //在作用域外呼叫 suspend fun
    //delay(1000L)

    // 取消 Coroutine
    //job.cancel()
}



fun coroutineAndroidLiveCycleCountDownClick(activity: AppCompatActivity,textView: TextView) {
    //lifecycleScope 是一個 CoroutineScope，它會在 Activity 或 Fragment 的生命週期結束時自動取消所有 Coroutine
    val job = activity.lifecycleScope.launch {
        // 倒數 10 秒
        for (i in 10 downTo 0) {
            // lifecycleScope有自動切換到主執行緒的功能
            textView.text = "coroutine倒數：$i 秒"
            // 延遲 1 秒
            delay(1000L)
        }
        // 倒數完成後更新 UI
        textView.text = "coroutine:倒數完成！"
    }

    // 取消 Coroutine
    //job.cancel()
}

