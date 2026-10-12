package io.zakkyhidayat.quran.data

import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/** Opens a public HTTPS download, following GitHub's asset redirects without permitting an insecure destination. */
internal fun openHttpsDownload(start: String): HttpURLConnection {
    var url = URL(start)
    if (url.protocol != "https") throw IOException("Alamat unduhan harus memakai HTTPS")
    repeat(6) {
        val conn = url.openConnection() as HttpURLConnection
        conn.connectTimeout = 15_000
        conn.readTimeout = 30_000
        conn.instanceFollowRedirects = false
        val code = conn.responseCode
        if (code in 300..399) {
            val location = conn.getHeaderField("Location")
            conn.disconnect()
            if (location == null) throw IOException("Pengalihan tanpa tujuan")
            url = URL(url, location)
            if (url.protocol != "https") throw IOException("Pengalihan ke alamat tidak aman")
        } else if (code == HttpURLConnection.HTTP_OK) {
            return conn
        } else {
            conn.disconnect()
            throw IOException("Server menjawab $code")
        }
    }
    throw IOException("Terlalu banyak pengalihan")
}
