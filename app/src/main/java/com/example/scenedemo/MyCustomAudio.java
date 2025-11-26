package com.example.scenedemo;

import android.content.Context;

import com.aispeech.ailog.AILog;
import com.aispeech.dui.dds.DDS;
import com.aispeech.dui.dds.agent.tts.bean.CustomAudioBean;
import com.aispeech.dui.dds.exceptions.DDSNotInitCompleteException;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MyCustomAudio {
    private static final String TAG = "MyCustomAudio";

    void setAudio(Context context) {

        //先把音频文件复制到外部存储的res目录下
        FileCopyUtils.copyAudio(context);


        // 获取外部存储路径
        File externalStorage = context.getExternalFilesDir(null);
        if (externalStorage == null) return;

        String baseAudioPath = externalStorage.getAbsolutePath() + File.separator + "res" + File.separator + "audio";

        // 读取 spk1 目录下的音频
        String spk1Path = baseAudioPath + File.separator + "spk1";

        // 读取 spk2 目录下的音频
        String spk2Path = baseAudioPath + File.separator + "spk2";

        //将音频设置给DDS
        try {
            DDS.getInstance().getAgent().getTTSEngine().setCustomAudio(getAudioListByPath(spk1Path)); // 设置
        } catch (DDSNotInitCompleteException e) {
            AILog.e(TAG, "setCustomAudio: " + e);
        }
    }

    private List<CustomAudioBean> getAudioListByPath(String audioPath) {
        List<CustomAudioBean> customAudioList = new ArrayList<>();
        String mappingFilePath = audioPath + File.separator + "mapping.json"; //mapping文件，记录了TTS和音频的对应关系
        File mappingFile = new File(mappingFilePath);
        if (mappingFile.exists() && mappingFile.isFile()) {
            StringBuilder sb = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new FileReader(mappingFile))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line);
                    sb.append(System.lineSeparator());
                }
                String jsonString = sb.toString().trim(); // 移除末尾的换行符
                AILog.d(TAG, "getAudioListByPath json:" + jsonString);
                JSONArray jsonArray = new JSONArray(new JSONTokener(jsonString));
                for (int i = 0; i < jsonArray.length(); i++) {
                    JSONObject jsonObject = jsonArray.getJSONObject(i);
                    String name = jsonObject.getString("name");
                    String path = jsonObject.getString("path");
                    String fullPath = audioPath + File.separator + path;
                    CustomAudioBean customAudioBean = new CustomAudioBean();
                    customAudioBean.setPath(fullPath);
                    customAudioBean.setName(name);
                    customAudioList.add(customAudioBean);
                }
            } catch (IOException | org.json.JSONException e) {
                AILog.e(TAG, "getAudioListByPath error: " + e);
            }
        }
        AILog.d(TAG, "getAudioListByPath result: " + customAudioList);
        return customAudioList;
    }
}
