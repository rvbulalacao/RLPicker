package com.barapido.rlpicker.api

import android.content.Context
import android.graphics.Bitmap
import android.util.LruCache
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.toolbox.*


class Rubian constructor(context: Context) {


    companion object {
        @Volatile
        private var INSTANCE: Rubian? = null
        val BASE_URL = "https://relis.codedisruptors.com/api"
        val ACCESS_KEY = "ab8c2d8e58124a9:6074c51025e332d"
        fun getInstance(context: Context) = INSTANCE ?: synchronized(this) {
            INSTANCE ?: Rubian(context).also {
                INSTANCE = it
            }
        }
    }
    val imageLoader: ImageLoader by lazy {
        ImageLoader(requestQueue,
            object: ImageLoader.ImageCache {
                private val cache = LruCache<String, Bitmap>(20)
                override fun getBitmap(url: String?): Bitmap? {
                    return cache.get(url)
                }

                override fun putBitmap(url: String?, bitmap: Bitmap?) {
                    cache.put(url, bitmap)
                }
            })
    }
    val requestQueue: RequestQueue by lazy {
        Volley.newRequestQueue(context.applicationContext)
    }

    fun <T> addToRequestQueue(req: Request<T>) {
        requestQueue.add(req)
    }
}