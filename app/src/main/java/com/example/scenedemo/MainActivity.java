package com.example.scenedemo;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.aispeech.dui.dds.DDS;
import com.aispeech.dui.dds.exceptions.DDSNotInitCompleteException;
import com.aispeech.widget.SpeechASRAnimTextView;

import org.json.JSONException;
import org.json.JSONObject;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class MainActivity extends AppCompatActivity {
    private static String TAG = "MainActivity";
    private SpeechASRAnimTextView speechASRAnimTextView;
    private Button test, charge,calling,callend,cleartext;
    private static final int PERMISSION_REQUEST_CODE = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        checkAndRequestPermissions();
        test = findViewById(R.id.test);
        charge = findViewById(R.id.charge);
        calling = findViewById(R.id.calling);
        callend = findViewById(R.id.callend);
        cleartext = findViewById(R.id.cleartext);
        speechASRAnimTextView = findViewById(R.id.tv_dialog_main_interact_default);
        initListener();
    }

    private void initListener() {
        test.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                try {
//                    DDS.getInstance().getAgent().sendText("I have a meeting at City Plaza");//这种方式英文不通
//                } catch (DDSNotInitCompleteException e) {
//                    e.printStackTrace();
//                }
            }
        });
        charge.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                JSONObject jsonObject = new JSONObject();
                try {
                    jsonObject.put("speakText", "Your charge was fifty six point one kilo watt hour, at eighteen point zero eight euros total. Bravo for choosing renewable energy!");
                    DDS.getInstance().getAgent().startDialog(jsonObject);
                } catch (JSONException | DDSNotInitCompleteException e) {
                    e.printStackTrace();
                }
            }
        });
        calling.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    DDS.getInstance().getAgent().getTTSEngine().speak("Dani is calling. Hold the answer button to enable transcription.", 1, "100", AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK);
                } catch (DDSNotInitCompleteException e) {
                    throw new RuntimeException(e);
                }
            }
        });
        callend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //调整上屏的速度时，可以修改SpeechASRAnimTextView 中的getShowTextDuration方法

                speechASRAnimTextView.setText("I'm");
                speechASRAnimTextView.setText("I'm going");
                speechASRAnimTextView.setText("I'm going to the ");
                speechASRAnimTextView.setText("I'm going to the supermarket ");
                speechASRAnimTextView.setText("I'm going to the supermarket to buy snacks");
                speechASRAnimTextView.setText("I'm going to the supermarket to buy snacks, but I ");
                speechASRAnimTextView.setText("I'm going to the supermarket to buy snacks, but I don't know ");
                speechASRAnimTextView.setText("I'm going to the supermarket to buy snacks, but I don't know what to buy");
            }
        });

        cleartext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                speechASRAnimTextView.setText("");
            }
        });
    }

    private void checkAndRequestPermissions() {
        // 检查是否同时拥有所有必要权限
        boolean hasPhonePermission = hasReadPhoneStatePermission();
        boolean hasAudioPermission = hasRecordAudioPermission();
        boolean hasStoragePermission = hasStoragePermission();

        if (!hasPhonePermission || !hasAudioPermission || !hasStoragePermission) {
            ActivityCompat.requestPermissions(this,
                    new String[]{
                            android.Manifest.permission.READ_PHONE_STATE,
                            android.Manifest.permission.RECORD_AUDIO,
                            android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
                            android.Manifest.permission.READ_EXTERNAL_STORAGE
                    },
                    PERMISSION_REQUEST_CODE);
        } else {
            startDDSService();
        }
    }


    private void startDDSService() {
        Intent intent = new Intent(this, DDSService.class);
        // 对于 Android 8.0+，需使用 startForegroundService() 启动前台服务（若 Service 是前台服务）
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            startForegroundService(intent);
        } else {
            startService(intent);
        }
    }

    /**
     * 检查是否拥有麦克风权限（RECORD_AUDIO）
     */
    public boolean hasRecordAudioPermission() {
        return ContextCompat.checkSelfPermission(this,
                android.Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED;
    }

    /**
     * 检查是否拥有 READ_PHONE_STATE 权限
     */
    public boolean hasReadPhoneStatePermission() {
        return ContextCompat.checkSelfPermission(this,
                android.Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED;
    }

    /**
     * 检查是否拥有存储权限
     */
    public boolean hasStoragePermission() {
        boolean writePermission = ContextCompat.checkSelfPermission(this,
                android.Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
        boolean readPermission = ContextCompat.checkSelfPermission(this,
                android.Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
        return writePermission && readPermission;
    }

    /**
     * 权限申请结果回调（处理所有权限的授权结果）
     */
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String[] permissions,
                                           int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        // 处理批量权限申请的结果
        if (requestCode == PERMISSION_REQUEST_CODE) {
            boolean allGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }

            if (allGranted) {
                startDDSService();
            }
        }
    }
}