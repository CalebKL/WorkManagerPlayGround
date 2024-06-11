package com.calebk.workmanagerplayground

import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import coil.compose.AsyncImage
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.work.Constraints
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.calebk.workmanagerplayground.ui.theme.WorkManagerPlayGroundTheme

class MainActivity : ComponentActivity() {
    private lateinit var workManager: WorkManager
    private val viewModel by viewModels<PhotoViewModel>()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        workManager = WorkManager.getInstance(applicationContext)
        enableEdgeToEdge()
        setContent {
            WorkManagerPlayGroundTheme {
                val workResult = viewModel.workId?.let { uuid ->
                    workManager.getWorkInfoByIdLiveData(uuid).observeAsState().value
                }
                LaunchedEffect(key1 = workResult?.outputData) {
                    if (workResult?.outputData != null) {
                        val filePath = workResult.outputData.getString(
                            PhotoCompressionWorker.KEY_RESULT_PATH
                        )
                        filePath?.let {
                            val bitmap = BitmapFactory.decodeFile(it)
                            viewModel.updateCompressedImageBitmap(bitmap)
                        }
                    }
                }
            }
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                viewModel.uncompressedUri?.let {
                    Text("Uncompressed Image")
                    AsyncImage(model = it, contentDescription = null)
                }
                Spacer(modifier = Modifier.height(16.dp))
                viewModel.compressedImageBitmap?.let {
                    Text("Compressed Image")
                    Image(bitmap = it.asImageBitmap(), contentDescription = null)
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        val uri = if (Build.VERSION.SDK_INT > Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
        } else {
            intent.getParcelableExtra(Intent.EXTRA_STREAM)
        } ?: return
        viewModel.updateUncompressedUri(uri)
        val request = OneTimeWorkRequestBuilder<PhotoCompressionWorker>()
            .setInputData(
                workDataOf(
                    PhotoCompressionWorker.KEY_URI to uri.toString(),
                    PhotoCompressionWorker.KEY_COMPRESSION_THRESHOLD to 1024 * 20L
                )
            )
            .build()
        viewModel.updateWorkId(request.id)
        workManager.enqueue(request)
    }
}
