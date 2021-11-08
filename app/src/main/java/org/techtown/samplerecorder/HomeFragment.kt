package org.techtown.samplerecorder

import android.annotation.SuppressLint
import android.content.Context.AUDIO_SERVICE
import android.content.Context.MODE_PRIVATE
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.MediaRecorder
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.os.SystemClock
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.view.animation.LinearInterpolator
import android.widget.Button
import android.widget.ImageView
import androidx.core.view.children
import androidx.fragment.app.Fragment
import org.techtown.samplerecorder.Audio.Queue
import org.techtown.samplerecorder.Audio.RecordService
import org.techtown.samplerecorder.Audio.TrackService
import org.techtown.samplerecorder.Util.DialogService.Companion.dialog
import org.techtown.samplerecorder.Util.LogUtil
import org.techtown.samplerecorder.Util.VolumeObserver
import org.techtown.samplerecorder.databinding.FragmentHomeBinding

class HomeFragment : Fragment(), View.OnClickListener {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!  // binding이 null 값을 갖음

    private val context by lazy { activity }

    @Suppress("DEPRECATION")
    private val volumeObserver by lazy { VolumeObserver(requireContext(), Handler()) }
    private val audioRecord by lazy { RecordService(requireContext()) }
    private val audioTrack by lazy { TrackService() }
    private val waveform by lazy { binding.viewWaveForm }
    private val switchButton by lazy { binding.switchButton }
    private var queue: Queue? = null

    private val dialogService by lazy { dialog(requireContext()) }

    private var startTime: Long = 0
    private var fileDrop = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        LogUtil.d(TAG, "")
        return binding.root
    }

    override fun onStart() {
        initUi()
        initState()
        setOnClickListener()
        LogUtil.d(TAG, "")
        super.onStart()
    }

    private fun initState() {
        activity?.volumeControlStream = volumeType

        // Switch Button 초기화
        val sharedPreferences = requireContext().getSharedPreferences(DATABASE, MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        fileDrop = sharedPreferences.getBoolean(FILE_DROP, false)
        if (fileDrop) switchButton.selectedTab = 0
        else switchButton.selectedTab = 1
        switchButton.setOnSwitchListener { position, _ ->
            fileDrop = position == 0
            with (editor) {
                putBoolean(FILE_DROP, fileDrop)
                apply()
            }
        }

        // Volume Content Observer 초기화
        requireContext().contentResolver.registerContentObserver(
            Settings.System.CONTENT_URI,
            true,
            volumeObserver
        )
    }

    @SuppressLint("SetTextI18n")
    private fun initUi() {
        val audioManager = context?.getSystemService(AUDIO_SERVICE) as AudioManager
        val nCurrentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        with (binding) {
            btnRecordSource.text = "${getString(R.string.source)}\n${getString(R.string.mic)}"
            btnPlayType.text = "${getString(R.string.type)}\n${getString(R.string.media)}"
            btnRecordChannel.text = "${getString(R.string.channel)}\n${getString(R.string.mono)}"
            btnPlayChannel.text = "${getString(R.string.channel)}\n${getString(R.string.mono)}"
            btnRecordSampleRate.text = "${getString(R.string.rate)}\n${getString(R.string.rate_16000)}"
            btnPlaySampleRate.text = "${getString(R.string.rate)}\n${getString(R.string.rate_16000)}"
            btnRecordBufferSize.text = "${getString(R.string.buffer_size)}\n${getString(R.string.buffer_size_1024)}"
            btnPlay.isEnabled = false
            btnPlayVolume.text = "${getString(R.string.volume)}\n${nCurrentVolume}"
        }
    }

    private fun setOnClickListener() {
        val context = this
        with (binding) {
            containerRecordSettings.children.forEach { btn ->
                btn.setOnClickListener(context)
            }
            containerPlaySettings.children.forEach { btn ->
                btn.setOnClickListener(context)
            }
            btnRecord.setOnClickListener(context)
            btnPlay.setOnClickListener(context)
        }
    }

    override fun onClick(view: View) {
        when (view.id) {
            R.id.btn_record -> {
                LogUtil.d(TAG, "")
                record()
            }
            R.id.btn_play -> play()
            R.id.btn_record_source -> {
                LogUtil.d(TAG, "")
                dialogService.create(getString(R.string.source))
            }
            R.id.btn_record_channel -> dialogService.create(getString(R.string.channel), getString(R.string.record))
            R.id.btn_record_sampleRate -> dialogService.create(getString(R.string.rate), getString(R.string.record))
            R.id.btn_record_bufferSize -> dialogService.create(getString(R.string.buffer_size))
            R.id.btn_play_type -> dialogService.create(getString(R.string.type))
            R.id.btn_play_channel -> dialogService.create(getString(R.string.channel), getString(R.string.play))
            R.id.btn_play_sampleRate -> dialogService.create(getString(R.string.rate), getString(R.string.play))
            R.id.btn_play_volume -> dialogService.create(getString(R.string.volume))
        }
    }

    private fun record() {
        if (!isRecording) {  // 녹음 버튼 클릭 시
            queue = Queue()
            isRecording = true
            audioRecord.start(queue!!, fileDrop)
            startRecording()
        } else {  // 정지 버튼 클릭 시
            isRecording = false
            audioRecord.stop(requireContext(), fileDrop)
            stopRecording()
        }
    }

    @Suppress("DEPRECATION")
    private fun startRecording() {
        // Waveform
        waveform.recreate()
        waveform.chunkColor = resources.getColor(R.color.red_record)

        // Record time
        startTime = SystemClock.elapsedRealtime()
        val recordMsg = recordHandler.obtainMessage().apply {
            what = MESSAGE_RECORD
        }
        recordHandler.sendMessage(recordMsg)

        // Ui
        with (binding) {
            textTimer.visibility = View.VISIBLE
            btnRecord.text = getString(R.string.stop)
            imgRecording.visibility = View.VISIBLE
            setAnimation(imgRecording, btnRecord)
            btnPlay.isEnabled = false
        }
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    private fun stopRecording() {
        // Record time
        recordHandler.removeMessages(0)

        // Ui
        with (binding) {
            with (imgRecording) {
                clearAnimation()
                visibility = View.INVISIBLE
            }
            with (btnRecord) {
                clearAnimation()
                text = getString(R.string.record)
            }
            btnPlay.isEnabled = true
        }
    }

    private fun play() {
        if (!isPlaying) {  // 재생 버튼 클릭 시
            isPlaying = true
            audioTrack.play(queue!!)
            startPlaying()
        } else {  // 정지 버튼 클릭 시
            isPlaying = false
            audioTrack.stop()
            stopPlaying()
        }
    }

    private fun startPlaying() {
        // Waveform
        waveform.recreate()
        waveform.chunkColor = resources.getColor(R.color.blue_play)

        // Play time
        startTime = SystemClock.elapsedRealtime()
        val playMsg = playHandler.obtainMessage().apply {
            what = MESSAGE_PLAY
        }
        playHandler.sendMessage(playMsg)

        // Ui
        with (binding) {
            imgPlaying.visibility = View.VISIBLE
            btnPlay.text = getString(R.string.stop)
            setAnimation(imgPlaying, btnPlay)
            btnRecord.isEnabled = false
        }
    }

    private fun stopPlaying() {
        LogUtil.d(TAG, "")
        playHandler.removeMessages(0)

        with (binding) {
            with (imgPlaying) {
                clearAnimation()
                visibility = View.INVISIBLE
            }
            with (btnPlay) {
                clearAnimation()
                text = getString(R.string.play)
            }
            btnRecord.isEnabled = true
        }
    }

    private fun setAnimation(imageView: ImageView, button: Button) {
        val animation: Animation = AlphaAnimation(1.0f, 0.0f).apply {
            duration = 500
            interpolator = LinearInterpolator()
            repeatCount = Animation.INFINITE
            repeatMode = Animation.REVERSE
        }
        imageView.startAnimation(animation)
        button.startAnimation(animation)
    }

    @SuppressLint("SetTextI18n")
    fun changeTextUi(setting: String, value: String, mode: String = "") {
        with (binding) {
            when (mode) {
                getString(R.string.record) -> {
                    when (setting) {
                        getString(R.string.channel) -> { btnRecordChannel.text = "$setting\n$value" }
                        getString(R.string.rate) -> { btnRecordSampleRate.text = "$setting\n$value" }
                    }
                }
                getString(R.string.play) -> {
                    when (setting) {
                        getString(R.string.channel) -> { btnPlayChannel.text = "$setting\n$value" }
                        getString(R.string.rate) -> { btnPlaySampleRate.text = "$setting\n$value" }
                    }
                }
                else -> {
                    when (setting) {
                        getString(R.string.source) -> { btnRecordSource.text = "$setting\n$value" }
                        getString(R.string.buffer_size) -> { btnRecordBufferSize.text = "$setting\n$value" }
                        getString(R.string.type) -> { btnPlayType.text = "$setting\n$value" }
                        getString(R.string.volume) -> { btnPlayVolume.text = "$setting\n$value" }
                    }
                }
            }
        }
    }

    val time: String
        get() {
            val nowTime = SystemClock.elapsedRealtime()
            val overTime = nowTime - startTime
            val min = overTime / 1000 / 60
            val sec = overTime / 1000 % 60
            val mSec = overTime % 1000 / 10
            return String.format("%02d : %02d : %02d", min, sec, mSec)
        }

    private var recordHandler: Handler = object : Handler() {
        @SuppressLint("HandlerLeak")
        override fun handleMessage(msg: Message) {
            binding.textTimer.text = time
            waveform.update(RecordService.recordWave)
            sendEmptyMessage(0)
        }
    }

    private var playHandler: Handler = object : Handler() {
        @SuppressLint("HandlerLeak")
        override fun handleMessage(msg: Message) {
            if (!emptyQueue) {
                binding.textTimer.text = time
                waveform.update(TrackService.playWave)
                sendEmptyMessage(0)
            } else {
                emptyQueue = false
                play()
            }
        }
    }

    override fun onDestroy() {
//        requireContext().contentResolver.unregisterContentObserver(volumeObserver)
        super.onDestroy()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val TAG = "HomeFragment"

        private const val PERMISSION_CODE = 1
        private const val MESSAGE_RECORD = 1
        private const val MESSAGE_PLAY = 2
        private const val DATABASE = "database"
        private const val FILE_DROP = "fileDrop"

        var isRecording = false
        var isPlaying = false
        var emptyQueue = false

        var source = MediaRecorder.AudioSource.MIC
        var type = AudioAttributes.USAGE_MEDIA
        var recordChannel = AudioFormat.CHANNEL_IN_MONO
        var playChannel = AudioFormat.CHANNEL_OUT_MONO
        var recordRate = 16000
        var playRate = 16000
        var bufferSize = 1024
        var volumeType = AudioManager.STREAM_MUSIC

        fun instance() = HomeFragment()

        var _binding: FragmentHomeBinding? = null
    }
}