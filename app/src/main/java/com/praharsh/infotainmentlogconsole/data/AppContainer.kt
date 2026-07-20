package com.praharsh.infotainmentlogconsole.data

import android.content.Context
import com.praharsh.infotainmentlogconsole.domain.MockSignalGenerator
import java.io.File

class AppContainer(context: Context, apiBaseUrl: String) {
    val generator = MockSignalGenerator()

    private val local = JsonLogFileStore(
        storageFile = File(context.filesDir, "infotainment-log-console.json"),
        exportDirectory = File(context.cacheDir, "exports"),
        legacyStorageFile = File(context.filesDir, "infotainment-log-console.tsv")
    )
    private val remote = HttpLogRemoteDataSource(apiBaseUrl)

    val repository: LogRepository = DefaultLogRepository(local, remote)
}
