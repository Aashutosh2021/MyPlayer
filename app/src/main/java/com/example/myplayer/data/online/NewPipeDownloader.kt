package com.example.myplayer.data.online

import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import org.schabi.newpipe.extractor.downloader.Downloader
import org.schabi.newpipe.extractor.downloader.Request
import org.schabi.newpipe.extractor.downloader.Response

class NewPipeDownloader(private val client: OkHttpClient) : Downloader() {
    override fun execute(request: Request): Response {
        val builder = okhttp3.Request.Builder()
            .url(request.url())
            
        request.headers().forEach { (key, values) ->
            values.forEach { value ->
                builder.addHeader(key, value)
            }
        }

        when (request.httpMethod()) {
            "GET" -> builder.get()
            "POST" -> builder.post(request.dataToSend()?.toRequestBody() ?: "".toRequestBody())
            "PUT" -> builder.put(request.dataToSend()?.toRequestBody() ?: "".toRequestBody())
            "DELETE" -> builder.delete(request.dataToSend()?.toRequestBody())
            "HEAD" -> builder.head()
            "OPTIONS" -> builder.method("OPTIONS", null)
            "TRACE" -> builder.method("TRACE", null)
            "PATCH" -> builder.patch(request.dataToSend()?.toRequestBody() ?: "".toRequestBody())
        }

        val okResponse = client.newCall(builder.build()).execute()
        
        val headers = mutableMapOf<String, List<String>>()
        okResponse.headers.names().forEach { name ->
            headers[name] = okResponse.headers.values(name)
        }

        return Response(
            okResponse.code,
            okResponse.message,
            headers,
            okResponse.body?.string() ?: "",
            request.url()
        )
    }
}
