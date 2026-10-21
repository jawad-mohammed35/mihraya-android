package com.mihraya.app.data.settings

import android.content.Context
import androidx.datastore.core.CorruptionException
import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.core.Serializer
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.dataStoreFile
import kotlinx.coroutines.CoroutineScope
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import java.io.InputStream
import java.io.OutputStream

private class JsonSerializer<T>(
    private val json: Json,
    private val serializer: KSerializer<T>,
    override val defaultValue: T,
) : Serializer<T> {

    override suspend fun readFrom(input: InputStream): T = try {
        json.decodeFromString(serializer, input.readBytes().decodeToString())
    } catch (e: SerializationException) {
        throw CorruptionException("Stored data could not be decoded", e)
    } catch (e: IllegalArgumentException) {
        throw CorruptionException("Stored data could not be decoded", e)
    }

    override suspend fun writeTo(t: T, output: OutputStream) {
        output.write(json.encodeToString(serializer, t).encodeToByteArray())
    }
}

fun <T> jsonDataStore(
    context: Context,
    fileName: String,
    json: Json,
    serializer: KSerializer<T>,
    default: T,
    scope: CoroutineScope,
): DataStore<T> = DataStoreFactory.create(
    serializer = JsonSerializer(json, serializer, default),
    corruptionHandler = ReplaceFileCorruptionHandler { default },
    scope = scope,
    produceFile = { context.dataStoreFile(fileName) },
)
