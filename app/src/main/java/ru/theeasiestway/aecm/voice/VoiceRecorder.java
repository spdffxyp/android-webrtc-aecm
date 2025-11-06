package ru.theeasiestway.aecm.voice;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;

public class VoiceRecorder {

    private AudioRecord recorder;
    private short[] buffer;

    public int getAudioSessionId() {
        if (recorder != null) {
            return recorder.getAudioSessionId();
        }
        return -1; // 或者抛出异常
    }

    public void start(int sampleRate, int frameSize) {
        int minBufferSize = AudioRecord.getMinBufferSize(sampleRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
        buffer = new short[frameSize];

        recorder = new AudioRecord(
                //MediaRecorder.AudioSource.VOICE_COMMUNICATION,
                MediaRecorder.AudioSource.MIC,
                sampleRate,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                minBufferSize * 2
        );
        recorder.startRecording();
    }

    public short[] frame() {
        if (recorder != null && recorder.getRecordingState() == AudioRecord.RECORDSTATE_RECORDING) {
            recorder.read(buffer, 0, buffer.length);
        }
        return buffer;
    }

    public void stop() {
        if (recorder != null && recorder.getState() == AudioRecord.STATE_INITIALIZED) {
            // 在 stop() 之前检查录制状态，避免崩溃
            if (recorder.getRecordingState() == AudioRecord.RECORDSTATE_RECORDING) {
                recorder.stop();
            }
        }
    }

    public void release() {
        if (recorder != null) {
            if (recorder.getRecordingState() == AudioRecord.RECORDSTATE_RECORDING) {
                recorder.stop();
            }
            recorder.release();
            recorder = null;
        }
    }
}