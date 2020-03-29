package com.breadwallet.presenter.activities.crc

import android.content.Context

object CrcUtils {

     fun readCities(context: Context, filename: String): String? {
        try {
            val inputStream = context.assets.open(filename)
            val size = inputStream.available()
            var buffer = ByteArray(size)
            inputStream.read(buffer)
            inputStream.close()

            return String(buffer)
        } catch (e : Exception) {
            e.printStackTrace()
        }

        return null
    }

}