package com.example.scenedemo;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.media.AudioFormat;
import android.os.Build;
import android.os.IBinder;

import com.aispeech.ailog.AILog;
import com.aispeech.dui.dds.DDS;
import com.aispeech.dui.dds.DDSAuthListener;
import com.aispeech.dui.dds.DDSConfig;
import com.aispeech.dui.dds.DDSConfigBuilder;
import com.aispeech.dui.dds.DDSInitListener;
import com.aispeech.dui.dds.agent.Agent;
import com.aispeech.dui.dds.agent.DMTaskCallback;
import com.aispeech.dui.dds.agent.wakeup.word.WakeupWord;
import com.aispeech.dui.dds.exceptions.DDSNotInitCompleteException;

import org.json.JSONObject;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import java.io.File;

public class DDSService extends Service {
    private static String TAG = "DDSService";
    private static final String CHANNEL_ID = "dds_service_channel";
    private static final int NOTIFICATION_ID = 1001;
    private DDSConfig ddsConfig;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        startForeground(NOTIFICATION_ID, createNotification());

        // 复制.bin文件到外部存储的res目录
        FileCopyUtils.copyBinFilesToSDCard(getApplicationContext());

        //初始化dds
        intDDS();

        //抓录音音频
        DDS.getInstance().setAudioDebug(true);
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "DDS 服务";
            String description = "用于维持 DDS 核心服务运行";
            int importance = NotificationManager.IMPORTANCE_LOW;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    private Notification createNotification() {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("DDS 服务运行中")
                .setContentText("正在提供语音交互能力")
                .setPriority(NotificationCompat.PRIORITY_LOW);

        return builder.build();
    }

    private void intDDS() {
        AILog.d(TAG, "intDDS");
        DDSConfigBuilder ddsConfigBuilder = new DDSConfigBuilder(
                "279632696",  // 产品ID
                "DEMO",   // 产品分支 prod/test/(自定义分支)
                "c8411d5fd54cc8411d5fd54c690c73e4",     // 产品的 apiKey，一个API Key只被指定的客户端使用，来源：DUI控制台-产品接入-授权管理
                "70262b0e599d6471d60e58426e4671ff", // 产品的 productKey,用于设备注册请求签名，来源：DUI控制台-产品接入-授权管理
                "964a8655c24776bb10c3d8b42796f194", // 产品的 productSecret,用于设备注册请求签名，来源：DUI控制台-产品接入-授权管理
                "duicore.zip"
        );
        ddsConfigBuilder.createWakeupBuilder()
                .setMicType(0);

//        //录音参数配置方式
//        ddsConfigBuilder.createWakeupBuilder()
//                .setMicType(2);
//        ddsConfigBuilder.createRecorderBuilder()
//                .setAudioSource(6) // 录音机参数: audioSource 录音机数据源类型
//                .setAudioSamplerate(16000)                         // 录音机参数: sampleRateInHz 录音时音频采样率
//                .setAudioChannelConf(4092)  // 录音机参数：channelConfig 录音机频道源类型
//                .setAudioFormat(AudioFormat.ENCODING_PCM_16BIT)    // 录音机参数：audioFormat 每个采样大小
//                .setAudioBufferSizeInByte(5120);                  // 录音机参数：bufferSizeInBytes 录音机的缓存大小
//        ddsConfigBuilder.addConfig("AEC_CHANNEL", 10);
//        ddsConfigBuilder.addConfig("MIC_ECHO_CHANNEL_NUM",10);

        //唤醒&信号处理资源
        ddsConfigBuilder.addConfig("CAR_FLAVOR", true);
        //.bin资源绝对路径，这里事先把.bin资源放到sdcard的res目录下
        File externalStorage = getExternalFilesDir(null);
        if (externalStorage != null) {
            File wakeupFile = new File(externalStorage, "res/wkp_aicar_tianqin_haiwai_20250513_v1.0.bin");
//            File beamformingFile = new File(externalStorage, "res/sspe_aec_nnbss_8chan_4mic_4ref_zeekrDC1E_001_v150_20240614_onThread_AEC4_doa1.bin");

            ddsConfigBuilder.addConfig(DDSConfig.K_WAKEUP_BIN, wakeupFile.getAbsolutePath());
//            ddsConfigBuilder.addConfig(DDSConfig.K_MIC_ARRAY_BEAMFORMING_CFG, beamformingFile.getAbsolutePath());
        }

        //设备唯一码
        ddsConfigBuilder.createCommonBuilder()
                .setCustomZip("product.zip")
                .setDeviceId("test1236666")
                .setDeviceName("test1236666");


        //国内服务器地址（alpha环境测试用）
        ddsConfigBuilder.addConfig("AUTH_SERVER", "https://auth.dui.ai");//# 授权服务
        ddsConfigBuilder.addConfig("CBRIDGE_ADDR", "wss://dds.alpha.duiopen.com/dds/v3");//# 语音服务，bridge的webwocket地址
        ddsConfigBuilder.addConfig("UPLOAD_ADDR", "https:/dds.alpha.duiopen.com/cinfo/v2");//#词库上传地址
        ddsConfigBuilder.addConfig("TTS_SERVER", "https://tts.alpha.duiopen.com/runtime/aggregation/synthesize");//# TTS服务地址

        //国外正式环境服务器
//        ddsConfigBuilder.addConfig("AUTH_SERVER", "https://auth.aispeech.com");//# 授权服务
//        ddsConfigBuilder.addConfig("CBRIDGE_ADDR", "wss://dds.aispeech.com/dds/v3");//# 语音服务，bridge的webwocket地址
//        ddsConfigBuilder.addConfig("UPLOAD_ADDR", "https:/dds.aispeech.com/cinfo/v2");//#词库上传地址
//        ddsConfigBuilder.addConfig("TTS_SERVER", "https://tts.aispeech.com/runtime/aggregation/synthesize");//# TTS服务地址

        //配置英文需要的参数
        ddsConfigBuilder.addConfig("LANGUAGE_MODEL_NAME", "English");
        ddsConfigBuilder.addConfig("LANGUAGE_TAG", "English");
        ddsConfigBuilder.addConfig("TTS_LANGUAGE", "5");
        ddsConfig = ddsConfigBuilder.build();

        DDS.getInstance().init(
                getApplicationContext(),
                ddsConfig,
                new DDSInitListener() {  // 资源、产品的初始化回调
                    @Override
                    public void onInitComplete(boolean isFull) {
                        AILog.d(TAG, "onInitComplete: " + isFull);
                        //初始化唤醒词
                        intWakeupWord();
                        try {
                            DDS.getInstance().setDebugMode(2);
                            DDS.getInstance().getAgent().setDuplexMode(Agent.DuplexMode.FULL_DUPLEX);// 全双工模式
                            DDS.getInstance().getAgent().getWakeupEngine().enableWakeup();//打开唤醒
                        } catch (DDSNotInitCompleteException e) {
                            throw new RuntimeException(e);
                        }
                        //初始化场景3
                        SceneTest.getInstance().init(getApplicationContext());
                        //拦截错误码播报
                        DDS.getInstance().getAgent().setDMTaskCallback(dmCallback);
                    }

                    @Override
                    public void onError(int what, String msg) {
                        AILog.d(TAG, "onError: " + what + ", error: " + msg);
                    }
                }, new DDSAuthListener() {   // 授权回调
                    @Override
                    public void onAuthSuccess() {
                        AILog.d(TAG, "onAuthSuccess");
                    }

                    @Override
                    public void onAuthFailed(String errId, String error) {
                        AILog.d(TAG, "onAuthFailed: " + errId + ", error:" + error);
                    }
                });
    }

    DMTaskCallback dmCallback = new DMTaskCallback() {
        @Override
        public JSONObject onDMTaskResult(JSONObject jsonObject, Type type) {
            AILog.d(TAG, "jsonObject: " + jsonObject + ", type: " + type);
            try {
                JSONObject errorObject = jsonObject.optJSONObject("error");
                int errId = errorObject.optInt("errId");
                AILog.d(TAG, "errId: " + errId);
//                71305 ： nlu null
//                71304, "asr null"
//                71309, "error retry max"

                if (errId == 71305 || errId == 71304 || errId == 71309) {
                    jsonObject.remove("nlg");
                    jsonObject.remove("display");
                    jsonObject.put("nlg", ""); //播报置空
                    jsonObject.put("display", "");  //显示置空
                }
                if (errId == 71309) { //"error retry max"
                    jsonObject.put("shouldEndSession", true);
                }

            } catch (Exception ignore) {
            }
            return jsonObject;
        }
    };

    private void intWakeupWord() {
        WakeupWord mainWord = new WakeupWord()
                .setPinyin("hai ao fei")
                .setWord("hai ao fei")
                .addGreeting("I'm here")
                .setThreshold("0.2");
        try {
            DDS.getInstance().getAgent().getWakeupEngine().addMainWakeupWord(mainWord);
        } catch (DDSNotInitCompleteException e) {
            throw new RuntimeException(e);
        }
    }

}