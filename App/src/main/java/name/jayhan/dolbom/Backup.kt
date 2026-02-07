package name.jayhan.dolbom

import android.content.Context
import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.contract.ActivityResultContracts.CreateDocument
import okio.IOException
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.InputStreamReader
import java.io.OutputStreamWriter

class Backup(
    private val origin: Backupable,
    private val context: Context,
    mainActivity: ComponentActivity,
) {
    private var onSuccess: ((Boolean) -> Unit)? = null

    val saverLauncher = mainActivity.registerForActivityResult(
        CreateDocument("text/plain"),
        SaveCallback()
    )
    fun save(
        onSuccess: ((Boolean) -> Unit)?
    ) {
        this.onSuccess = onSuccess
        saverLauncher.launch("Dolbom_${origin.filenamePart}_${nowDateTimeFilename()}.txt")
    }

    inner class SaveCallback():
        ActivityResultCallback<Uri?>
    {
        override fun onActivityResult(result: Uri?) {
            var success = false
            try {
                val contentResolver = context.contentResolver
                if (result != null) {
                    contentResolver.openOutputStream(result).use { outputStream ->
                        BufferedWriter(OutputStreamWriter(outputStream)).use { writer ->
                            writer.write(origin.toText())
                        }
                    }
                    success = true
                }
            } catch(_: IOException) {}
            onSuccess?.invoke(success)
            onSuccess = null
        }
    }

    val loaderLauncher = mainActivity.registerForActivityResult(
        ActivityResultContracts.OpenDocument(),
        LoadCallback()
    )

    fun load(
        onSuccess: ((Boolean) -> Unit)?
    ) {
        this.onSuccess = onSuccess
        loaderLauncher.launch(arrayOf("text/plain"))
    }

    inner class LoadCallback():
        ActivityResultCallback<Uri?>
    {
        override fun onActivityResult(result: Uri?) {
            var success = false
            if (result != null) {
                val contentResolver = context.contentResolver
                try {
                    contentResolver.openInputStream(result).use { inputStream ->
                        BufferedReader(InputStreamReader(inputStream)).use { reader ->
                            val text = reader.readText()
                            success = origin.fromText(text)
                        }
                    }
                } catch (_: java.io.IOException) { }
            }
            onSuccess?.invoke(success)
            onSuccess = null
        }
    }
}

interface Backupable {
    fun toText(): String
    fun fromText(text: String): Boolean
    val filenamePart: String
}
