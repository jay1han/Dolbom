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

class FileManager(
    private val context: Context,
    mainActivity: ComponentActivity
) {
    constructor(context: Context) : this(context, ComponentActivity())

    private val saverLauncher = mainActivity.registerForActivityResult(
            CreateDocument("text/plain"),
            SaveIndicatorsCallback()
        )
    fun saveIndicators() {
        saverLauncher.launch("DolbomBackup.txt")
    }
    
    private val loaderLauncher = mainActivity.registerForActivityResult(
        ActivityResultContracts.OpenDocument(),
        LoadIndicatorsCallback()
        )
    fun loadIndicators() {
        loaderLauncher.launch(arrayOf("text/plain"))
    }
    
    inner class SaveIndicatorsCallback():
        ActivityResultCallback<Uri?>
    {
        override fun onActivityResult(result: Uri?) {
            val contentResolver = context.contentResolver
            if (result != null) {
                contentResolver.openOutputStream(result).use { outputStream ->
                    BufferedWriter(OutputStreamWriter(outputStream)).use { writer ->
                        writer.write(Indicators.toText())
                    }
                }
            }
        }
    }

    inner class LoadIndicatorsCallback():
        ActivityResultCallback<Uri?>
    {
        override fun onActivityResult(result: Uri?) {
            val contentResolver = context.contentResolver
            if (result != null) {
                contentResolver.openInputStream(result).use { inputStream ->
                    BufferedReader(InputStreamReader(inputStream)).use { reader ->
                        val text = reader.readText()
                        Indicators.fromText(text)
                    }
                }
            }
        }
    }
}
