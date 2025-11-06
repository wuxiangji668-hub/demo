package com.example.scenedemo;

import android.content.Context;

import com.aispeech.ailog.AILog;
import com.aispeech.dui.dds.DDS;
import com.aispeech.dui.dsk.duiwidget.CommandObserver;

import org.json.JSONException;
import org.json.JSONObject;

public class SceneThree {
    private final String TAG = "SceneThree";
    public static final String DEMO_RESPONSE = "demo.response";
    private Context mContext;
    private String[] nativeCmdArr = {DEMO_RESPONSE};
    private SceneThree() {
    }

    public static SceneThree getInstance() {
        return SceneThreeSceneThree.instance;
    }

    private static final class SceneThreeSceneThree {
        public static SceneThree instance = new SceneThree();
    }


    public void init(Context context) {
        AILog.d(TAG, "init called with: context = [" + context + "]");
        mContext = context;
        //订阅demo.response
        DDS.getInstance().getAgent().subscribe(nativeCmdArr, commandObserver);
    }

    private CommandObserver commandObserver = new CommandObserver() {
        @Override
        public void onCall(final String command, final String data) {
            AILog.d(TAG, "init called with: command = [" + command + "]");
            try {
                JSONObject  jsonData = new JSONObject(data);
                String flow = jsonData.optString("flow");
                String task = jsonData.optString("task");

                if(flow.equals("3")){
                    if(task.equals("1")){
                        //USER:"""I have a meeting at City Plaza""
                        //TTS:"There isn't enough charge for the trip. Shall we look for a charging station?"
                        //不用执行动作
                    }
                    if(task.equals("2")){
                        //USER:"""Yes, find one with renewable energy and food nearby""
                        //TTS:"I see three renewable energy charging stations with restaurants within range. Which one shall we go to?"

                        //TODO 1展示附近三个有餐厅或便利店的再生能源充电站
                        AILog.d(TAG, "展示附近三个有餐厅或便利店的再生能源充电站");

                    } else if (task.equals("3")) {
                        //USER:""The second one"
                        //TTS:"OK, I've added that a stop"

                        //TODO 2规划路线：终点为 xx 写字楼，途径点为用户选择充电站
                        AILog.d(TAG, "规划路线：终点为 xx 写字楼，途径点为用户选择充电站");
                        //TODO 3充完电之后.主动拉起语音播报，并唤醒语音 语音需要已经退出 (sceneDemo ui上的按钮)

                    } else if (task.equals("4")) {
                        //USER:""Can you look for parking?""
                        //TTS:I found these three parking locations. Which one would you like to choose?

                        //TODO 4展示目的地附近三个停车场
                        AILog.d(TAG, "展示目的地附近三个停车场");

                    } else if (task.equals("5")){
                        //USER:"""Reserve the first one for two hours""
                        //TTS:"Got it. I've reserved parking from two thirty to four thirty PM. Updating directions."

                        //TODO 5更新目的地为停车场
                        AILog.d(TAG, "更新目的地为停车场");

                    }

                }

            } catch (JSONException e) {
                throw new RuntimeException(e);
            }

        }
    };
}
