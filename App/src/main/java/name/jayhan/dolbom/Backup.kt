package name.jayhan.dolbom

import android.content.Context
import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.contract.ActivityResultContracts.CreateDocument
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.InputStreamReader
import java.io.OutputStreamWriter

class Backup(
    private val origin: Backupable,
    private val context: Context,
    mainActivity: ComponentActivity
) {
    constructor(
        origin: Backupable,
        context: Context,
    ) : this(
        origin,
        context,
        ComponentActivity()
    )

    private val saverLauncher = mainActivity.registerForActivityResult(
        CreateDocument("text/plain"),
        SaveCallback()
    )
    fun save() {
        saverLauncher.launch("Dolbom_${origin.filenamePart}_${nowDateTimeFilename()}.txt")
    }

    private val loaderLauncher = mainActivity.registerForActivityResult(
        ActivityResultContracts.OpenDocument(),
        LoadCallback()
    )
    fun load() {
        loaderLauncher.launch(arrayOf("text/plain"))
    }

    inner class SaveCallback():
        ActivityResultCallback<Uri?>
    {
        override fun onActivityResult(result: Uri?) {
            val contentResolver = context.contentResolver
            if (result != null) {
                contentResolver.openOutputStream(result).use { outputStream ->
                    BufferedWriter(OutputStreamWriter(outputStream)).use { writer ->
                        writer.write(origin.toText())
                    }
                }
            }
        }
    }

    inner class LoadCallback():
        ActivityResultCallback<Uri?>
    {
        override fun onActivityResult(result: Uri?) {
            val contentResolver = context.contentResolver
            if (result != null) {
                contentResolver.openInputStream(result).use { inputStream ->
                    BufferedReader(InputStreamReader(inputStream)).use { reader ->
                        val text = reader.readText()
                        origin.fromText(text)
                    }
                }
            }
        }
    }
}

interface Backupable {
    fun toText(): String
    fun fromText(text: String)
    val filenamePart: String
}
