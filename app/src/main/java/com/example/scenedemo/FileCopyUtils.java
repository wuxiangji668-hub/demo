package com.example.scenedemo;

import android.content.Context;
import android.os.Environment;
import android.util.Log;

import com.aispeech.ailog.AILog;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class FileCopyUtils {
    private static final String TAG = "FileCopyUtils";


    /**
     * 复制.bin文件到外部存储的res目录
     */
    public static void copyBinFilesToSDCard(Context context) {
        // 使用正确的路径
        String targetDir = "res";

        // 复制第一个.bin文件
        boolean result1 = FileCopyUtils.copyAssetFileToSDCard(context,
                "sspe_aec_nnbss_8chan_4mic_4ref_zeekrDC1E_001_v150_20240614_onThread_AEC4_doa1.bin",
                targetDir);

        // 复制第二个.bin文件
        boolean result2 = FileCopyUtils.copyAssetFileToSDCard(context,
                "wkp_aicar_zeekr_uni_v1.3.4_20240920_3tasks_vp.bin",
                targetDir);

        AILog.d(TAG, "Copy .bin files result: " + result1 + ", " + result2);

        // 检查一下文件路径
        File externalStorage = context.getExternalFilesDir(null);
        if (externalStorage != null) {
            File wakeupFile = new File(externalStorage, "res/wkp_aicar_zeekr_uni_v1.3.4_20240920_3tasks_vp.bin");
            File beamformingFile = new File(externalStorage, "res/sspe_aec_nnbss_8chan_4mic_4ref_zeekrDC1E_001_v150_20240614_onThread_AEC4_doa1.bin");

            AILog.d(TAG, "Wakeup file path: " + wakeupFile.getAbsolutePath());
            AILog.d(TAG, "Beamforming file path: " + beamformingFile.getAbsolutePath());
        }
    }


    /**
     * 将assets目录下的文件复制到外部存储的res目录
     *
     * @param context       上下文
     * @param assetFileName assets目录下的文件名
     * @param targetDir     目标目录
     * @return 是否复制成功
     */
    private static boolean copyAssetFileToSDCard(Context context, String assetFileName, String targetDir) {
        try {
            // 检查外部存储是否可用
            if (!isExternalStorageWritable()) {
                Log.e(TAG, "External storage is not writable");
                return false;
            }

            // 获取应用特定的外部存储目录
            File externalStorage = context.getExternalFilesDir(null);
            if (externalStorage == null) {
                Log.e(TAG, "External storage directory is null");
                return false;
            }

            Log.d(TAG, "External storage directory: " + externalStorage.getAbsolutePath());

            // 创建目标目录 - 在应用特定目录下创建res文件夹
            File dir = new File(externalStorage, targetDir);
            Log.d(TAG, "Attempting to create directory: " + dir.getAbsolutePath());

            // 创建目标目录
            if (!dir.exists()) {
                Log.d(TAG, "Creating directory: " + dir.getAbsolutePath());
                if (!dir.mkdirs()) {
                    Log.e(TAG, "Failed to create directory: " + dir.getAbsolutePath());
                    return false;
                }
            }

            // 目标文件路径
            File outFile = new File(dir, assetFileName);

            // 如果文件已存在，直接返回true
            if (outFile.exists()) {
                Log.d(TAG, "File already exists: " + outFile.getAbsolutePath());
                return true;
            }

            // 打开assets文件输入流
            InputStream inputStream = context.getAssets().open(assetFileName);

            // 创建文件输出流
            OutputStream outputStream = new FileOutputStream(outFile);

            // 复制文件
            byte[] buffer = new byte[1024];
            int length;
            while ((length = inputStream.read(buffer)) > 0) {
                outputStream.write(buffer, 0, length);
            }

            // 关闭流
            outputStream.flush();
            outputStream.close();
            inputStream.close();

            Log.d(TAG, "File copied successfully to: " + outFile.getAbsolutePath());
            return true;
        } catch (IOException e) {
            Log.e(TAG, "Failed to copy file: " + assetFileName + ", error: " + e.getMessage());
            e.printStackTrace();
            return false;
        } catch (Exception e) {
            Log.e(TAG, "Unexpected error when copying file: " + assetFileName + ", error: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 检查外部存储是否可写
     *
     * @return 是否可写
     */
    private static boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        boolean writable = Environment.MEDIA_MOUNTED.equals(state);
        Log.d(TAG, "External storage state: " + state + ", writable: " + writable);
        return writable;
    }
}