package ru.theeasiestway.aecm;

import android.Manifest;
import android.content.pm.PackageManager;
import android.media.audiofx.AcousticEchoCanceler;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import ru.theeasiestway.aecm.voice.VoicePlayer;
import ru.theeasiestway.aecm.voice.VoiceRecorder;
import ru.theeasiestway.libaecm.AEC;

public class MainActivity extends AppCompatActivity {

    private final String TAG = "AECM_MainActivity";

    private enum AecMode {
        NONE,
        LIB_AECM,
        ANDROID_AEC
    }

    private AecMode currentAecMode = AecMode.NONE;

    private int SAMPLE_RATE = 8000;
    private int FRAME_SIZE = 160;

    private Button playBtn;
    private Button stopBtn;
    private SeekBar seekBarSampleRate;
    private TextView textViewSeekBarSampleRate;
    private TextView textViewSeekBarFrameSizeLabel;
    private SeekBar seekBarAggressiveMode;
    private TextView textViewSeekBarAggressiveMode;
    private SeekBar seekBarEchoLength;
    private TextView textViewSeekBarEchoLength;

    private AEC aec;
    private AcousticEchoCanceler androidAec;

    private VoiceRecorder voiceRecorder;
    private VoicePlayer voicePlayer;
    private volatile boolean stop;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        aec = new AEC();

        playBtn = findViewById(R.id.playBtn);
        playBtn.setOnClickListener(v -> { if (hasRecAudioPermission()) startPlay(); });

        stopBtn = findViewById(R.id.stopBtn);
        stopBtn.setOnClickListener(v -> {
            stopBtn.setVisibility(View.GONE);
            playBtn.setVisibility(View.VISIBLE);
            stop();
        });

        RadioGroup radioGroupAec = findViewById(R.id.radio_group_aec);
        radioGroupAec.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.radio_aec_none) {
                currentAecMode = AecMode.NONE;
            } else if (checkedId == R.id.radio_aec_lib) {
                currentAecMode = AecMode.LIB_AECM;
            } else if (checkedId == R.id.radio_aec_android) {
                currentAecMode = AecMode.ANDROID_AEC;
            }
        });

        textViewSeekBarSampleRate = findViewById(R.id.text_view_seek_bar_sample_rate_label);
        seekBarSampleRate = findViewById(R.id.seek_bar_sample_rate);
        seekBarSampleRate.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (progress <= 8000) {
                    progress = 8000; // Let's set a floor value
                    seekBar.setProgress(progress);
                }
                if (progress > 8000 && progress <= 16000) {
                    progress = 16000;
                    seekBar.setProgress(progress);
                }
                String s = progress + "hz";
                textViewSeekBarSampleRate.setText(s);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                SAMPLE_RATE = seekBar.getProgress() <= 8000 ? 8000 : 16000;
            }
        });
        seekBarSampleRate.setProgress(SAMPLE_RATE);

        textViewSeekBarFrameSizeLabel = findViewById(R.id.text_view_seek_bar_frame_size_label);
        SeekBar seekBarFrameSize = findViewById(R.id.seek_bar_frame_size);
        seekBarFrameSize.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (progress <= 80) {
                    progress = 80;
                    seekBar.setProgress(progress);
                }
                if (progress > 80) {
                    progress = 160;
                    seekBar.setProgress(160);
                }
                String s = progress + "";
                textViewSeekBarFrameSizeLabel.setText(s);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                FRAME_SIZE = seekBar.getProgress() <= 80 ? 80 : 160;
            }
        });
        seekBarFrameSize.setProgress(FRAME_SIZE);

        textViewSeekBarAggressiveMode = findViewById(R.id.text_view_seek_bar_aggressive_mode_label);
        seekBarAggressiveMode = findViewById(R.id.seek_bar_aggressive_mode);
        seekBarAggressiveMode.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                textViewSeekBarAggressiveMode.setText(progress + "");
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });
        seekBarAggressiveMode.setProgress(4);

        textViewSeekBarEchoLength = findViewById(R.id.text_view_seek_bar_echo_length_label);
        seekBarEchoLength = findViewById(R.id.seek_bar_echo_length);
        seekBarEchoLength.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (progress < 1) {
                    seekBarEchoLength.setProgress(1);
                    return;
                }
                textViewSeekBarEchoLength.setText(progress + "ms");
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });
        seekBarEchoLength.setProgress(20);
    }

    private void startPlay() {
        playBtn.setVisibility(View.GONE);
        stopBtn.setVisibility(View.VISIBLE);
        play();
    }

    @RequiresApi(23)
    private boolean hasRecAudioPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return true;
        else if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_DENIED) {
            requestPermissions(new String[] {Manifest.permission.RECORD_AUDIO}, 1);
            return false;
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1 && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startPlay();
        }
    }

    private void play() {
        stop = false;

        // 1. 创建并启动 VoiceRecorder
        voiceRecorder = new VoiceRecorder();
        voiceRecorder.start(SAMPLE_RATE, FRAME_SIZE); // 使用修改后的 start 方法

        // 2. 从 VoiceRecorder 获取 Audio Session ID
        int audioSessionId = voiceRecorder.getAudioSessionId();
        if (audioSessionId == -1) {
            Log.e(TAG, "Failed to get Audio Session ID from VoiceRecorder.");
            Toast.makeText(this, "Recorder init failed", Toast.LENGTH_SHORT).show();
            // 清理并返回
            stop();
            playBtn.setVisibility(View.VISIBLE);
            stopBtn.setVisibility(View.GONE);
            return;
        }
        Log.d(TAG, "Audio Session ID from Recorder: " + audioSessionId);

        // 3. 创建并启动 VoicePlayer，传入获取到的 session ID
        voicePlayer = new VoicePlayer();
        voicePlayer.start(SAMPLE_RATE, audioSessionId); // 使用修改后的 start 方法

        // 4. 根据模式，使用同一个 session ID 初始化 AEC
        switch (currentAecMode) {
            case LIB_AECM:
                aec.setSampFreq(SAMPLE_RATE == 8000 ? AEC.SamplingFrequency.FS_8000Hz : AEC.SamplingFrequency.FS_16000Hz);
                aec.setAecmMode(getAggressiveMode());
                Log.d(TAG, "Using libaecm for echo cancellation.");
                break;
            case ANDROID_AEC:
                if (AcousticEchoCanceler.isAvailable()) {
                    androidAec = AcousticEchoCanceler.create(audioSessionId); // <-- 使用正确的 session ID
                    if (androidAec != null) {
                        androidAec.setEnabled(true);
                        Log.d(TAG, "Android AcousticEchoCanceler created and enabled. Status: " + androidAec.getEnabled());
                    } else {
                        Log.e(TAG, "Failed to create Android AcousticEchoCanceler.");
                        Toast.makeText(this, "Failed to create Android AEC", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Log.w(TAG, "Android AcousticEchoCanceler is not available on this device.");
                    Toast.makeText(this, "Android AEC not available", Toast.LENGTH_SHORT).show();
                }
                break;
            case NONE:
                Log.d(TAG, "AEC is disabled.");
                break;
        }

        // 5. 启动处理线程
        new Thread(() -> {
            while (!stop) {
                short[] frame = voiceRecorder.frame();

                if (currentAecMode == AecMode.LIB_AECM) {
                    aec.farendBuffer(frame, FRAME_SIZE);
                    short[] resultFrame = aec.echoCancellation(frame, null, FRAME_SIZE, seekBarEchoLength.getProgress());
                    voicePlayer.write(resultFrame);
                } else {
                    // 对于 Android AEC，你不需要做任何处理，系统会自动处理
                    // 对于 NONE 模式，直接播放录制的数据
                    voicePlayer.write(frame);
                }
            }
        }).start();
    }

    private AEC.AggressiveMode getAggressiveMode() {
        int progress = seekBarAggressiveMode.getProgress();
        switch (progress) {
            case 0:
                return AEC.AggressiveMode.MILD;
            case 1:
                return AEC.AggressiveMode.MEDIUM;
            case 2:
                return AEC.AggressiveMode.HIGH;
            case 3:
                return AEC.AggressiveMode.AGGRESSIVE;
            case 4:
                return AEC.AggressiveMode.MOST_AGGRESSIVE;
        }
        return AEC.AggressiveMode.AGGRESSIVE;
    }

    private void stop() {
        stop = true;
        if (voiceRecorder != null) {
            voiceRecorder.stop();
            voiceRecorder.release();
            voiceRecorder = null;
        }

        if (voicePlayer != null) {
            voicePlayer.stopPlaying();
            voicePlayer.release();
            voicePlayer = null;
        }

        if (androidAec != null) {
            androidAec.setEnabled(false);
            androidAec.release();
            androidAec = null;
            Log.d(TAG, "Android AcousticEchoCanceler released.");
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stop();
        if (aec != null) {
            aec.close();
        }
    }
}