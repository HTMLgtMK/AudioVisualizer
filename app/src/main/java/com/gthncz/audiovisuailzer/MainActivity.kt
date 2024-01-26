package com.gthncz.audiovisuailzer

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.core.app.ActivityCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.gthncz.audiovisuailzer.player.LocalMediaPlayer
import com.gthncz.audiovisuailzer.player.PlayerState
import com.gthncz.audiovisuailzer.ui.theme.AudioVisuailzerTheme
import com.gthncz.audiovisuailzer.utils.PermissionUtil
import com.gthncz.audiovisuailzer.visualizer.AudioDanceView
import com.gthncz.audiovisuailzer.visualizer.ProcessAudioFeatureCallback
import com.gthncz.audiovisuailzer.visualizer.VisualizerController
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    companion object {
        private const val TAG = "MainActivity"
        private val BASE_PERMISSIONS = arrayOf(
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.MODIFY_AUDIO_SETTINGS,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.READ_MEDIA_AUDIO
        )
    }

    private val mMainViewModel: MainViewModel by lazy {
        ViewModelProvider(this)[MainViewModel::class.java]
    }

    private var mPlayer: LocalMediaPlayer? = null

    private val mPermissionRequestLauncher = PermissionUtil.registerRequestPermissionLauncher(
        this,
        *BASE_PERMISSIONS,
    )  {results->
        if (results.values.all { it }) {
            if (mPlayer?.getPlayerState() == PlayerState.STATE_STARTED) {
                VisualizerController.onPlayerPrepared(mPlayer?.getAudioSession() ?: 0)
            } else {
                startPlay()
            }
        }
    }

    private val mFileSelectorLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (it.resultCode == Activity.RESULT_OK) {
            val data = it.data?.data ?: return@registerForActivityResult
            val songPath = com.gthncz.audiovisuailzer.utils.FileUtils(this).getPath(data)
            Log.i(TAG, "[FileSelector] path: $songPath")
            mMainViewModel.updateSongPath(songPath)
        }
    }

    private val mAudioFeatureProcessor: ProcessAudioFeatureCallback = { feature ->
        Log.i(TAG, "audio feature: ${feature.size}, ${feature.joinToString()}")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AudioVisuailzerTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Column {
                        Greeting("Android", modifier = Modifier.wrapContentHeight())

                        Spacer(modifier = Modifier.padding(vertical = 10.dp))
                        
                        Button(
                            modifier = Modifier
                                .wrapContentHeight()
                                .padding(10.dp),
                            onClick = { checkAndRequestPermissions {  } }
                        ) {
                            Text(text = "CheckPermissions")
                        }
                        
                        Spacer(modifier = Modifier.padding(vertical = 10.dp))

                        SongPlayerArea(mMainViewModel)

                        Spacer(modifier = Modifier.padding(vertical = 10.dp))

                        val songFilePath = remember {
                            val state = mutableStateOf<String?>(null)
                            lifecycleScope.launch {
                                mMainViewModel.songPathState.collect { songPath->
                                    state.value = songPath
                                }
                            }
                            state
                        }
                        FileSelector(name = "SelectFile: ${songFilePath.value}", modifier = Modifier.wrapContentHeight()) {
                            val intent = Intent(Intent.ACTION_GET_CONTENT)
                            intent.setType("audio/*")
                            mFileSelectorLauncher.launch(Intent.createChooser(intent, "select audio"))
                        }

                        Spacer(modifier = Modifier.padding(vertical = 10.dp))

                        VisualizerArea(mMainViewModel)
                    }
                }
            }
        }

        lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                VisualizerController.addProcessAudioFeatureCallback(mAudioFeatureProcessor)
            }
        }
    }

    override fun onStart() {
        super.onStart()
        lifecycleScope.launch {
            mMainViewModel.playerState.collect { }
        }
        lifecycleScope.launch {
            mMainViewModel.songPathState.collect { }
        }
        lifecycleScope.launch {
            mMainViewModel.visualizerState.collect { }
        }
    }

    private fun startPlay() {
        checkAndRequestPermissions {
            mMainViewModel.playOrPause()
        }
    }

    override fun onDestroy() {
        VisualizerController.onPlayerReleased()
        mPlayer?.release()
        super.onDestroy()
    }

    private fun checkAndRequestPermissions(goOnWork: () -> Unit) {
        when {
            PermissionUtil.checkPermissionGranted(this, *BASE_PERMISSIONS) -> {
                Log.i(TAG, "permissions has been granted.")
                goOnWork.invoke()
            }
            ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.RECORD_AUDIO) -> {
                val dialog = AlertDialog.Builder(this)
                    .setMessage("record_audio permission is required for audio visualizer.")
                    .setPositiveButton("OK", object: DialogInterface.OnClickListener {
                        override fun onClick(p0: DialogInterface?, p1: Int) {
                            val intent = Intent(Settings.ACTION_PRIVACY_SETTINGS)
                            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            startActivity(intent)
                        }
                    })
                    .setNeutralButton("I Know", null)
                    .create()
                dialog.show()
            }
            else -> {
                mPermissionRequestLauncher.launch(BASE_PERMISSIONS)
            }
        }
    }
}

@Composable
fun VisualizerArea(viewModel: MainViewModel) {
    val lifecycleScope = rememberCoroutineScope()
    Column(
        Modifier
            .height(200.dp)
            .fillMaxWidth()) {
        AndroidView(factory = {context->
            AudioDanceView(context).also {danceView->
                VisualizerController.addProcessAudioFeatureCallback { featureData ->
                    danceView.updateFeatureData(featureData)
                }
                lifecycleScope.launch {
                    viewModel.visualizerState.collect {
                        danceView.initRenderer(it)
                    }
                }
            }
        }, modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight()
        )
    }
}


@Composable
fun SongPlayerArea(viewModel: MainViewModel) {
    val lifecycleScope = rememberCoroutineScope()
    ConstraintLayout(modifier = Modifier.wrapContentHeight()) {
        Button(modifier = Modifier
            .wrapContentSize()
            .padding(10.dp), onClick = { viewModel.playOrPause() }) {

            val textState = remember {
                val state = mutableStateOf<String>("")
                lifecycleScope.launch {
                    viewModel.playerState.collect {playerState ->
                        val ret = when (playerState) {
                            PlayerState.STATE_STARTED -> "Pause"
                            PlayerState.STATE_PAUSED -> "Resume"
                            else -> "StartPlay"
                        }
                        state.value = ret
                    }
                }
                state
            }

            Text(text = textState.value)
        }
    }
}

@Composable
fun FileSelector(name: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = modifier,
    ) {
        Text(text = name)
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    AudioVisuailzerTheme {
        Greeting("Android")
    }
}