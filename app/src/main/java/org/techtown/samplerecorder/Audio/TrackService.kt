package org.techtown.samplerecorder.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.widget.ImageView
import android.widget.SeekBar
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.techtown.samplerecorder.home.HomeFragment.Companion.bufferSize
import org.techtown.samplerecorder.home.HomeFragment.Companion.emptyQueue
import org.techtown.samplerecorder.home.HomeFragment.Companion.isPlaying
import org.techtown.samplerecorder.home.HomeFragment.Companion.playChannel
import org.techtown.samplerecorder.home.HomeFragment.Companion.playRate
import org.techtown.samplerecorder.home.HomeFragment.Companion.type
import org.techtown.samplerecorder.list.ListViewHolder.Companion.FLAG_CAN_PLAY
import org.techtown.samplerecorder.list.ListViewHolder.Companion.FLAG_PAUSE_STATE
import org.techtown.samplerecorder.list.ListFragment.Companion.BUTTON_PLAY
import org.techtown.samplerecorder.util.AppModule.dataToShort
import java.io.RandomAccessFile

class TrackService {
    private var audioTrack: AudioTrack? = null
    private var job: Job? = null
    var pausePoint: Long? = 0

    fun create() {
        if (audioTrack == null) {
            audioTrack = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(type)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(playRate)
                        .setChannelMask(playChannel)
                        .build()
                )
                .setBufferSizeInBytes(bufferSize * 2)
                .build()
        }
    }

    fun play(queue: Queue) {
        create()

        var audioData: ByteArray?
        job = CoroutineScope(Dispatchers.IO).launch {
            audioTrack!!.play()
            while (isPlaying) {
                // Audio Track Read
                audioData = queue.dequeue()
                audioTrack!!.write(audioData!!, 0, bufferSize)

                // Endpoint of empty queue
                if (queue.isEmpty) {
                    emptyQueue = true
                    queue.copy()
                    break
                }

                playWave = 0  // Waveform
                for (i in audioData!!.indices) playWave = dataToShort(audioData)
            }
        }
    }

    @Suppress("BlockingMethodInNonBlockingContext")
    fun play(file: RandomAccessFile, seekBar: SeekBar, button: ImageView, seekPoint: Int = -1) {
        val audioData = ByteArray(bufferSize)
        job = CoroutineScope(Dispatchers.Default).launch {
            audioTrack!!.play()
            if (FLAG_PAUSE_STATE) {
                FLAG_PAUSE_STATE = false
                if (seekPoint == -1) file.seek(pausePoint!!)
                else file.seek(seekPoint.toLong())
            }
            var i = 0
            while (i != -1) {
                i = file.read(audioData)
                audioTrack!!.write(audioData, 0, i)
                seekBar.progress = file.filePointer.toInt()
                if (FLAG_PAUSE_STATE) {
                    pausePoint = file.filePointer
                    break
                }
            }
            file.close()
            stop()
            button.setImageDrawable(BUTTON_PLAY)
            FLAG_CAN_PLAY = true
        }
    }

    fun stop() {
        if (audioTrack != null) {
            audioTrack!!.flush()
            audioTrack!!.stop()
            audioTrack!!.release()
            audioTrack = null
            job!!.cancel()
        }
    }

    companion object {
        private const val TAG = "TrackService"
        var playWave = 0
    }
}