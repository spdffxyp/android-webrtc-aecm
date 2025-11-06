package ru.theeasiestway.aecm.voice;

import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Build;
import android.util.Log;

public class VoicePlayer {

    private AudioTrack audioTrack;

    private void createAudioTrack(int sampleRate, int audioSessionId) {
        int minBufferSize = AudioTrack.getMinBufferSize(sampleRate, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // 经测试AUDIO_SESSION_ID_GENERATE不影响降噪效果
            // AcousticEchoCanceler由于不可用，在启动AcousticEchoCanceler时是否有效未测试
            // audioSessionId = AudioManager.AUDIO_SESSION_ID_GENERATE;
            audioTrack = new AudioTrack.Builder()
                    .setAudioAttributes(new AudioAttributes.Builder()
                            // 使用USAGE_VOICE_COMMUNICATION
                            // 收到这个信号后，Android 的音频框架 (AudioFlinger/AudioPolicy) 会自动启用一系列底层的信号处理算法，这通常是硬件或系统级别的优化，包括但不限于：
                            // 内置的回声消除 (AEC)：即使你没有在应用层通过 AcousticsEchoCanceler 显式创建，很多设备也会在 VOICE_COMMUNICATION 模式下默认开启一个基础级别的回声抑制。
                            // 自动增益控制 (AGC)：防止声音过大或过小。
                            // 噪声抑制 (NS)：降低背景噪音。
                            .setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
                            // 使用USAGE_MEDIA则会引起啸叫
                            // .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                            .build())
                    .setAudioFormat(new AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(sampleRate)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build())
                    .setBufferSizeInBytes(minBufferSize)
                    .setTransferMode(AudioTrack.MODE_STREAM)
                    .setSessionId(audioSessionId) // <-- 使用传入的 session ID
                    .build();
        } else {
            // Deprecated but necessary for older APIs
            audioTrack = new AudioTrack(
                    // 使用STREAM_VOICE_CALL
                    // 收到这个信号后，Android 的音频框架 (AudioFlinger/AudioPolicy) 会自动启用一系列底层的信号处理算法，这通常是硬件或系统级别的优化，包括但不限于：
                    // 内置的回声消除 (AEC)：即使你没有在应用层通过 AcousticsEchoCanceler 显式创建，很多设备也会在 VOICE_COMMUNICATION 模式下默认开启一个基础级别的回声抑制。
                    // 自动增益控制 (AGC)：防止声音过大或过小。
                    // 噪声抑制 (NS)：降低背景噪音。
                    AudioManager.STREAM_VOICE_CALL,
                    // 使用STREAM_MUSIC则会引起啸叫
                    // AudioManager.STREAM_MUSIC,
                    sampleRate,
                    AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    minBufferSize,
                    AudioTrack.MODE_STREAM,
                    audioSessionId); // <-- 使用传入的 session ID
        }
    }

    public void start(int sampleRate, int audioSessionId) {
        createAudioTrack(sampleRate, audioSessionId);
        if (audioTrack.getState() == AudioTrack.STATE_INITIALIZED) {
            audioTrack.play();
        } else {
            Log.e("VoicePlayer", "AudioTrack failed to initialize.");
            // 这里的重试逻辑可能不是最佳实践，但我们保留它
            for (int i = 0; i < 3; i++) {
                createAudioTrack(sampleRate, audioSessionId);
                if (audioTrack.getState() == AudioTrack.STATE_INITIALIZED) {
                    audioTrack.play();
                    break;
                }
            }
        }
    }

    public void write(short[] frame) {
        if (frame == null || audioTrack == null || audioTrack.getState() != AudioTrack.STATE_INITIALIZED) return;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            audioTrack.write(frame, 0, frame.length, AudioTrack.WRITE_BLOCKING);
        } else {
            audioTrack.write(frame, 0, frame.length);
        }
    }

    public void stopPlaying() {
        if (audioTrack != null && audioTrack.getState() == AudioTrack.STATE_INITIALIZED) {
            audioTrack.stop();
            audioTrack.flush();
        }
    }

    public void release() {
        if (audioTrack != null) {
            audioTrack.release();
            audioTrack = null;
        }
    }
}