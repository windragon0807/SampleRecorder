package org.techtown.samplerecorder.Util

import android.annotation.SuppressLint
import android.content.Context
import android.content.Context.AUDIO_SERVICE
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.MediaRecorder
import android.text.Html.fromHtml
import android.view.Gravity
import android.view.View
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import org.techtown.samplerecorder.HomeFragment
import org.techtown.samplerecorder.HomeFragment.Companion._binding
import org.techtown.samplerecorder.HomeFragment.Companion.bufferSize
import org.techtown.samplerecorder.HomeFragment.Companion.playChannel
import org.techtown.samplerecorder.HomeFragment.Companion.playRate
import org.techtown.samplerecorder.HomeFragment.Companion.recordChannel
import org.techtown.samplerecorder.HomeFragment.Companion.recordRate
import org.techtown.samplerecorder.HomeFragment.Companion.source
import org.techtown.samplerecorder.HomeFragment.Companion.type
import org.techtown.samplerecorder.HomeFragment.Companion.volumeType
import org.techtown.samplerecorder.MainActivity
import org.techtown.samplerecorder.R
import org.techtown.samplerecorder.databinding.FragmentHomeBinding

class DialogService private constructor(private val context: Context) {

    private val mainFragment = HomeFragment.instance()
    private val mainActivity = context as MainActivity
    private val sourceList = arrayOf(
        context.getString(R.string.defaults),
        context.getString(R.string.mic),
        context.getString(R.string.voice_communication),
        context.getString(R.string.voice_performance),
        context.getString(R.string.voice_recognition),
        context.getString(R.string.unprocessed)
    )
    private val channelList = arrayOf(
        context.getString(R.string.mono),
        context.getString(R.string.stereo)
    )
    private val rateList = arrayOf(
        context.getString(R.string.rate_8000),
        context.getString(R.string.rate_11025),
        context.getString(R.string.rate_16000),
        context.getString(R.string.rate_22050),
        context.getString(R.string.rate_44100)
    )
    private val bufferList = arrayOf(
        context.getString(R.string.buffer_size_512),
        context.getString(R.string.buffer_size_1024),
        context.getString(R.string.buffer_size_2048)
    )
    private val typeList = arrayOf(
        context.getString(R.string.ring),
        context.getString(R.string.media),
        context.getString(R.string.alarm),
        context.getString(R.string.notification),
        context.getString(R.string.system)
    )

    private var sourceIndex = 1
    private var recordChannelIndex = 0
    private var recordRateIndex = 2
    private var bufferSizeIndex = 1
    private var typeIndex = 1
    private var playChannelIndex = 0
    private var playRateIndex = 2

    fun create(setting: String, mode: String = "") {
        when (setting) {
            context.getString(R.string.exit) -> {
                exitDialog()
            }
            context.getString(R.string.source) -> {
                // TODO HomeFragment binding = null 되는 최초 시점
                sourceDialog()
            }
            context.getString(R.string.channel) -> {
                channelDialog(mode)
            }
            context.getString(R.string.rate) -> {
                rateDialog(mode)
            }
            context.getString(R.string.buffer_size) -> {
                bufferSizeDialog()
            }
            context.getString(R.string.type) -> {
                typeDialog()
            }
            context.getString(R.string.volume) -> {
                volumeDialog()
            }
        }
    }

    @Suppress("DEPRECATION")
    private fun exitDialog() {
        val builder = dialogBuilder()
        builder.setTitle(context.getString(R.string.exit))
            .setMessage(context.getString(R.string.exit_message))
            .setIcon(ContextCompat.getDrawable(context, R.drawable.ic_exit))
            .setPositiveButton(fromHtml("<font color='${context.getColor(R.color.blue_exit_yes)}'>${context.getString(
                R.string.yes
            )}</font>")) { _, _ ->
                ActivityCompat.finishAffinity(context as MainActivity)
            }
            .setNegativeButton(fromHtml("<font color='${context.getColor(R.color.red_exit_no)}'>${context.getString(
                R.string.no
            )}</font>")) { dialog, _ ->
                dialog.cancel()
            }
        showDialog(builder)
    }

    @SuppressLint("SetTextI18n")
    private fun sourceDialog() {
        val builder = dialogBuilder()
        builder.setTitle(context.getString(R.string.source))
            .setIcon(ContextCompat.getDrawable(context, R.drawable.ic_source))
            .setSingleChoiceItems(sourceList, sourceIndex) { _, which ->
                when (sourceList[which]) {
                    context.getString(R.string.defaults) -> {
                        source = MediaRecorder.AudioSource.DEFAULT
                        mainFragment.changeTextUi(context.getString(R.string.source), context.getString(R.string.defaults))
                    }
                    context.getString(R.string.mic) -> {
                        source = MediaRecorder.AudioSource.MIC
                        mainFragment.changeTextUi(context.getString(R.string.source), context.getString(R.string.mic))
                    }
                    context.getString(R.string.voice_communication) -> {
                        source = MediaRecorder.AudioSource.VOICE_COMMUNICATION
                        mainFragment.changeTextUi(context.getString(R.string.source), context.getString(R.string.voice_communication))
                    }
                    context.getString(R.string.voice_performance) -> {
                        source = MediaRecorder.AudioSource.VOICE_PERFORMANCE
                        mainFragment.changeTextUi(context.getString(R.string.source), context.getString(R.string.voice_performance))
                    }
                    context.getString(R.string.voice_recognition) -> {
                        source = MediaRecorder.AudioSource.VOICE_RECOGNITION
                        mainFragment.changeTextUi(context.getString(R.string.source), context.getString(R.string.voice_recognition))
                    }
                    context.getString(R.string.unprocessed) -> {
                        source = MediaRecorder.AudioSource.UNPROCESSED
                        mainFragment.changeTextUi(context.getString(R.string.source), context.getString(R.string.unprocessed))
                    }
                }
                sourceIndex = which
            }
        showDialog(builder)
    }

    @SuppressLint("SetTextI18n")
    private fun channelDialog(mode: String) {
        val builder = dialogBuilder()
        builder.setTitle(context.getString(R.string.channel))
        when (mode) {
            context.getString(R.string.record) -> {
                builder.setIcon(ContextCompat.getDrawable(context, R.drawable.ic_record_channel))
                    .setSingleChoiceItems(channelList, recordChannelIndex) { _, which ->
                        when (channelList[which]) {
                            context.getString(R.string.mono) -> {
                                recordChannel = AudioFormat.CHANNEL_IN_MONO
                                mainFragment.changeTextUi(context.getString(R.string.channel), context.getString(
                                    R.string.mono
                                ), mode)
                            }
                            context.getString(R.string.stereo) -> {
                                recordChannel = AudioFormat.CHANNEL_IN_STEREO
                                mainFragment.changeTextUi(context.getString(R.string.channel), context.getString(
                                    R.string.stereo
                                ), mode)
                            }
                        }
                        recordChannelIndex = which
                    }
            }
            context.getString(R.string.play) -> {
                builder.setIcon(ContextCompat.getDrawable(context, R.drawable.ic_play_channel))
                    .setSingleChoiceItems(channelList, playChannelIndex) { _, which ->
                        when (channelList[which]) {
                            context.getString(R.string.mono) -> {
                                playChannel = AudioFormat.CHANNEL_OUT_MONO
                                mainFragment.changeTextUi(context.getString(R.string.channel), context.getString(
                                    R.string.mono
                                ), mode)
                            }
                            context.getString(R.string.stereo) -> {
                                playChannel = AudioFormat.CHANNEL_OUT_STEREO
                                mainFragment.changeTextUi(context.getString(R.string.channel), context.getString(
                                    R.string.stereo
                                ), mode)
                            }
                        }
                        playChannelIndex = which
                    }

            }

        }
        showDialog(builder)
    }

    @SuppressLint("SetTextI18n")
    private fun rateDialog(mode: String) {
        val builder = dialogBuilder()
        builder.setTitle(context.getString(R.string.rate))
        when (mode) {
            context.getString(R.string.record) -> {
                builder.setIcon(ContextCompat.getDrawable(context, R.drawable.ic_record_samplerate))
                    .setSingleChoiceItems(rateList, recordRateIndex) { _, which ->
                        when (rateList[which]) {
                            context.getString(R.string.rate_8000) -> {
                                recordRate = SAMPLE_RATE_8000
                                mainFragment.changeTextUi(context.getString(R.string.rate), context.getString(
                                    R.string.rate_8000
                                ), mode)
                            }
                            context.getString(R.string.rate_11025) -> {
                                recordRate = SAMPLE_RATE_11025
                                mainFragment.changeTextUi(context.getString(R.string.rate), context.getString(
                                    R.string.rate_11025
                                ), mode)
                            }
                            context.getString(R.string.rate_16000) -> {
                                recordRate = SAMPLE_RATE_16000
                                mainFragment.changeTextUi(context.getString(R.string.rate), context.getString(
                                    R.string.rate_16000
                                ), mode)
                            }
                            context.getString(R.string.rate_22050) -> {
                                recordRate = SAMPLE_RATE_22050
                                mainFragment.changeTextUi(context.getString(R.string.rate), context.getString(
                                    R.string.rate_22050
                                ), mode)
                            }
                            context.getString(R.string.rate_44100) -> {
                                recordRate = SAMPLE_RATE_44100
                                mainFragment.changeTextUi(context.getString(R.string.rate), context.getString(
                                    R.string.rate_44100
                                ), mode)
                            }
                        }
                        recordRateIndex = which
                    }
            }
            context.getString(R.string.play) -> {
                builder.setIcon(ContextCompat.getDrawable(context, R.drawable.ic_play_samplerate))
                    .setSingleChoiceItems(rateList, playRateIndex) { _, which ->
                        when (rateList[which]) {
                            context.getString(R.string.rate_8000) -> {
                                playRate = SAMPLE_RATE_8000
                                mainFragment.changeTextUi(context.getString(R.string.rate), context.getString(
                                    R.string.rate_8000
                                ), mode)
                            }
                            context.getString(R.string.rate_11025) -> {
                                playRate = SAMPLE_RATE_11025
                                mainFragment.changeTextUi(context.getString(R.string.rate), context.getString(
                                    R.string.rate_11025
                                ), mode)
                            }
                            context.getString(R.string.rate_16000) -> {
                                playRate = SAMPLE_RATE_16000
                                mainFragment.changeTextUi(context.getString(R.string.rate), context.getString(
                                    R.string.rate_16000
                                ), mode)
                            }
                            context.getString(R.string.rate_22050) -> {
                                playRate = SAMPLE_RATE_22050
                                mainFragment.changeTextUi(context.getString(R.string.rate), context.getString(
                                    R.string.rate_22050
                                ), mode)
                            }
                            context.getString(R.string.rate_44100) -> {
                                playRate = SAMPLE_RATE_44100
                                mainFragment.changeTextUi(context.getString(R.string.rate), context.getString(
                                    R.string.rate_44100
                                ), mode)
                            }
                        }
                        playRateIndex = which
                    }
            }
        }
        showDialog(builder)
    }

    @SuppressLint("SetTextI18n")
    private fun bufferSizeDialog() {
        val builder = dialogBuilder()
        builder.setTitle(context.getString(R.string.buffer_size))
            .setIcon(ContextCompat.getDrawable(context, R.drawable.ic_buffersize))
            .setSingleChoiceItems(bufferList, bufferSizeIndex) { _, which ->
                when (bufferList[which]) {
                    context.getString(R.string.buffer_size_512) -> {
                        bufferSize = BUFFER_SIZE_512
                        mainFragment.changeTextUi(context.getString(R.string.buffer_size), context.getString(
                            R.string.buffer_size_512
                        ))
                    }
                    context.getString(R.string.buffer_size_1024) -> {
                        bufferSize = BUFFER_SIZE_1024
                        mainFragment.changeTextUi(context.getString(R.string.buffer_size), context.getString(
                            R.string.buffer_size_1024
                        ))
                    }
                    context.getString(R.string.buffer_size_2048) -> {
                        bufferSize = BUFFER_SIZE_2048
                        mainFragment.changeTextUi(context.getString(R.string.buffer_size), context.getString(
                            R.string.buffer_size_2048
                        ))
                    }
                }
                bufferSizeIndex = which
            }
        showDialog(builder)
    }

    @SuppressLint("SetTextI18n")
    private fun typeDialog() {
        val builder = dialogBuilder()
        builder.setTitle(context.getString(R.string.type))
            .setIcon(ContextCompat.getDrawable(context, R.drawable.ic_type))
            .setSingleChoiceItems(typeList, typeIndex) { _, which ->
                when (typeList[which]) {
                    context.getString(R.string.ring) -> {
                        type = AudioAttributes.USAGE_NOTIFICATION_RINGTONE
                        volumeType = AudioManager.STREAM_RING
                        mainFragment.changeTextUi(context.getString(R.string.type), context.getString(R.string.ring))
                    }
                    context.getString(R.string.media) -> {
                        type = AudioAttributes.USAGE_MEDIA
                        volumeType = AudioManager.STREAM_MUSIC
                        mainFragment.changeTextUi(context.getString(R.string.type), context.getString(R.string.media))
                    }
                    context.getString(R.string.alarm) -> {
                        type = AudioAttributes.USAGE_ALARM
                        volumeType = AudioManager.STREAM_ALARM
                        mainFragment.changeTextUi(context.getString(R.string.type), context.getString(R.string.alarm))
                    }
                    context.getString(R.string.notification) -> {
                        type = AudioAttributes.USAGE_NOTIFICATION
                        volumeType = AudioManager.STREAM_NOTIFICATION
                        mainFragment.changeTextUi(context.getString(R.string.type), context.getString(R.string.notification))
                    }
                    context.getString(R.string.system) -> {
                        type = AudioAttributes.USAGE_ASSISTANCE_SONIFICATION
                        volumeType = AudioManager.STREAM_SYSTEM
                        mainFragment.changeTextUi(context.getString(R.string.type), context.getString(R.string.system))
                    }
                }
                typeIndex = which
                mainActivity.volumeControlStream = volumeType
            }
        showDialog(builder)
    }

    private fun volumeDialog() {
        val viewSeekbar = mainActivity.layoutInflater.inflate(R.layout.seekbar_volume, null)
        val seekBar = viewSeekbar.findViewById<View>(R.id.seekbar_volume) as SeekBar
        val textView = viewSeekbar.findViewById<View>(R.id.text_seekbar) as TextView
        val imageView = viewSeekbar.findViewById<View>(R.id.img_seekbar) as ImageView

        val builder = dialogBuilder()
        builder.setTitle(context.getString(R.string.volume)).setView(viewSeekbar)
        seekbarSetting(seekBar, textView, imageView)
        showDialog(builder)
    }

    private fun seekbarSetting(seekBar: SeekBar, volumeText: TextView, image: ImageView) {
        val audioManager = mainActivity.getSystemService(AUDIO_SERVICE) as AudioManager
        val currentVolume = audioManager.getStreamVolume(volumeType)
        seekBar.min = 1
        seekBar.max = audioManager.getStreamMaxVolume(volumeType)
        seekBar.progress = currentVolume
        seekBarUi(currentVolume, volumeText, image)
        seekBar.setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                audioManager.setStreamVolume(volumeType, progress, 0)
                seekBarUi(progress, volumeText, image)
            }
            override fun onStopTrackingTouch(seekBar: SeekBar) { }
            override fun onStartTrackingTouch(seekBar: SeekBar) { }
        })
    }

    private fun seekBarUi(volume: Int, volumeText: TextView, image: ImageView) {
        when {
            volume >= 13 -> {
                volumeText.setTextColor(mainActivity.resources.getColor(R.color.red_record))
                image.setImageResource(R.drawable.ic_volume_loud)
            }
            volume in 10..12 -> {
                volumeText.setTextColor(mainActivity.resources.getColor(R.color.blue_play))
                image.setImageResource(R.drawable.ic_volume_loud)
            }
            else -> {
                volumeText.setTextColor(mainActivity.resources.getColor(R.color.blue_play))
                image.setImageResource(R.drawable.ic_volume_small)
            }
        }
        volumeText.text = volume.toString()
    }

    private fun dialogBuilder(): AlertDialog.Builder {
        return AlertDialog.Builder(
            context,
            android.R.style.Theme_DeviceDefault_Light_Dialog_NoActionBar
        )
    }

    private fun showDialog(builder: AlertDialog.Builder) {
        val dialog = builder.create()
        dialog.window!!.setGravity(Gravity.CENTER)
        dialog.show()
    }

    companion object {
        private const val TAG = "DialogService"
        const val SAMPLE_RATE_8000 = 8000
        const val SAMPLE_RATE_11025 = 11025
        const val SAMPLE_RATE_16000 = 16000
        const val SAMPLE_RATE_22050 = 22050
        const val SAMPLE_RATE_44100 = 44100
        const val BUFFER_SIZE_512 = 512
        const val BUFFER_SIZE_1024 = 1024
        const val BUFFER_SIZE_2048 = 2048

        fun dialog(context: Context) = DialogService(context)
    }
}