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
     * 复制assets目录下的audio目录到外部存储的res目录
     */
    public static void copyAudio(Context context) {
        String sourceDir = "audio";
        String targetDir = "res" + File.separator + "audio";
        boolean result = copyAssetDirToSDCard(context, sourceDir, targetDir);

        AILog.d(TAG, "Copy audio directory result: " + result);
    }

    /**
     * 复制assets目录下的bin目录到外部存储的res目录
     */
    public static void copyBinFilesToSDCard(Context context) {
        // 使用正确的路径
        String sourceDir = "bin"; // assets目录下的bin文件夹
        String targetDir = "res"; // 外部存储的目标目录

        // 复制整个bin目录
        boolean result = copyAssetDirToSDCard(context, sourceDir, targetDir);

        AILog.d(TAG, "Copy bin directory result: " + result);
    }

    /**
     * 递归复制assets目录下的文件夹到外部存储
     *
     * @param context       上下文
     * @param assetDirPath  assets目录下的源文件夹路径
     * @param targetDir     外部存储的目标目录
     * @return 是否复制成功
     */
    private static boolean copyAssetDirToSDCard(Context context, String assetDirPath, String targetDir) {
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

            // 获取assets目录下的文件列表
            String[] files = context.getAssets().list(assetDirPath);
            if (files == null || files.length == 0) {
                Log.w(TAG, "No files found in assets directory: " + assetDirPath);
                return false;
            }

            Log.d(TAG, "Found " + files.length + " items in " + assetDirPath);

            boolean allSuccess = true;
            for (String fileName : files) {
                String assetFilePath = assetDirPath + "/" + fileName;

                // 检查是文件还是目录
                String[] subFiles = context.getAssets().list(assetFilePath);
                if (subFiles != null && subFiles.length > 0) {
                    // 是目录，递归复制
                    Log.d(TAG, "Processing directory: " + assetFilePath);
                    boolean result = copyAssetDirToSDCard(context, assetFilePath, targetDir + "/" + fileName);
                    allSuccess = allSuccess && result;
                } else {
                    // 是文件，直接复制
                    Log.d(TAG, "Processing file: " + assetFilePath);
                    boolean result = copyAssetFileToSDCard(context, assetFilePath, targetDir);
                    allSuccess = allSuccess && result;
                }
            }

            return allSuccess;
        } catch (IOException e) {
            Log.e(TAG, "Failed to copy directory: " + assetDirPath + ", error: " + e.getMessage());
            e.printStackTrace();
            return false;
        } catch (Exception e) {
            Log.e(TAG, "Unexpected error when copying directory: " + assetDirPath + ", error: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }


    /**
     * 将assets目录下的文件复制到外部存储
     *
     * @param context        上下文
     * @param assetFilePath  assets目录下的文件路径（可能包含子目录）
     * @param targetDir      目标目录
     * @return 是否复制成功
     */
    private static boolean copyAssetFileToSDCard(Context context, String assetFilePath, String targetDir) {
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

            // 提取文件名（去除路径部分）
            String fileName = assetFilePath.substring(assetFilePath.lastIndexOf("/") + 1);
            
            // 目标文件路径
            File outFile = new File(dir, fileName);

            // 如果文件已存在，直接返回true
            if (outFile.exists()) {
                Log.d(TAG, "File already exists: " + outFile.getAbsolutePath());
                return true;
            }

            // 打开assets文件输入流
            InputStream inputStream = context.getAssets().open(assetFilePath);

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
            Log.e(TAG, "Failed to copy file: " + assetFilePath + ", error: " + e.getMessage());
            return false;
        } catch (Exception e) {
            Log.e(TAG, "Unexpected error when copying file: " + assetFilePath + ", error: " + e.getMessage());
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