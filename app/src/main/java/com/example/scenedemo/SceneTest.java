package com.example.scenedemo;

import android.content.Context;

import com.aispeech.ailog.AILog;
import com.aispeech.dui.dds.DDS;
import com.aispeech.dui.dsk.duiwidget.CommandObserver;

import org.json.JSONException;
import org.json.JSONObject;

public class SceneTest {
    private final String TAG = "SceneTest";
    public static final String DEMO_RESPONSE = "demo.response";
    private Context mContext;
    private String[] nativeCmdArr = {DEMO_RESPONSE};
    private SceneTest() {
    }

    public static SceneTest getInstance() {
        return SceneTestInstance.instance;
    }

    private static final class SceneTestInstance  {
        public static SceneTest instance = new SceneTest();
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

                switch (flow){
                    case "1":
                        SceneOne(task);
                        break;
                    case "2":
                        SceneTwo(task);
                        break;
                    case "3":
                        SceneThree(task);
                        break;
                }
            } catch (JSONException e) {
                throw new RuntimeException(e);
            }

        }
    };

    /**
     * 场景1
     */
    public void SceneOne(String task) {
        if (task.equals("1")) {
            //TODO  唤醒
            //USER:"""What's on my calendar?""
            //TTS:"You are picking up Mateo at Flughafen Stuttgart at four fifty PM, and then dinner at Posthörnle at 7. Would you like directions for these?"
            //不用执行动作

        } else if (task.equals("2")) {
            //USER:"""let's go to the airport first""
            //TTS:""""OK. We'll be there in twenty four minutes""

            //TODO 1设置 Flughafenstraße 32, Stuttgart, 70629, Germany 为目的地
            AILog.d(TAG, "设置 Flughafenstraße 32, Stuttgart, 70629, Germany 为目的地");

        } else if (task.equals("3")) {
            //TODO  唤醒
            //USER:"""have you got anything for me to listen to?""
            //TTS:I've got some political and financial news for you, since you tend to like those. Shall I read them to you?
            //不用执行动作

        } else if (task.equals("4")) {
            //USER:"""No, find something more relaxing""
            //TTS:OK, here you go: {CONTENT} CONTENT由云端定

        } else if (task.equals("5")) {
            //TODO  唤醒
            //USER:"""I'm hungry. Let's head to dinner.""
            //TTS:"Got it. Here are directions for Posthörnle"

            //TODO 2设置 Pliensaustraße 56, Esslingen am Neckar, 73728, Germany 为目的地
            AILog.d(TAG, "设置 Pliensaustraße 56, Esslingen am Neckar, 73728, Germany 为目的地");

        } else if (task.equals("6")) {
            //USER:"""Is it easy to park around there?""
            //TTS:"I found three parking locations near Posthörnle. Which one would you like?"

            //TODO 3"展示目的地附近三个停车场
            //B+B Parkhaus
            //Martinstraße 15, 73728 Esslingen am Neckar, Germany
            //Das Es!
            //Martinstraße 15, 73728 Esslingen am Neckar, Germany
            //Parkplatz
            //73728 Esslingen, Germany"
            AILog.d(TAG, "展示目的地附近三个停车场");
            
        } else if (task.equals("7")) {
            //USER:""""book the first one for ninety minutes""
            //TTS:"OK, I've reserved parking from six thirty to eight PM. Heading to B+B Parkhaus near Posthörnle."

            //TODO 4设置 Kiesstraße 1, Esslingen am Neckar, 73728, Germany 为目的地
            AILog.d(TAG, "设置 Kiesstraße 1, Esslingen am Neckar, 73728, Germany 为目的地");

        } else if (task.equals("8")) {
            //TODO  唤醒
            //USER:"""There's an accident ahead""
            //TTS:"OK, there's a five minute detour. Would you like to take it?"
            //不用执行动作

        } else if (task.equals("9")) {
            //USER:"""Sure""
            //TTS:"Route updated."

            //TODO 重新算路；导航内部播报导航指引，调用DDS.getInstance().getAgent().getTTSEngine().speak（）
        }

    }

    /**
     * 场景2
     */
    public void SceneTwo(String task) {
        //来电 点击来电按钮播报
        //下一步 电话挂断，点击挂断按钮 会有打字机文本效果的展示

        //TODO 下一步，主动拉起语音并播报
        //jsonObject.put("speakText", "I've added something to your to-do list. Go to the supermarket to buy snacks for the movie. I have some shopping suggestions if you'd want.");
        //DDS.getInstance().getAgent().startDialog(jsonObject);

        if(task.equals("2")){
            //USER:"Sure, let's hear them"
            //TTS:"You are going to need popcorn for a movie, or some chips and salsa, with a case of coke."

            //TODO 展示购物推荐UI
            AILog.d(TAG, "展示购物推荐UI");

        } else if (task.equals("3")){
            //USER:""Let's go with popcorn and coke, and some chocolate
            //TTS:"OK, I'm adding a ten pack Coca Cola, Seeberger oven popcorn, and some dark chocolate to your cart"
            //不用执行动作

        } else if (task.equals("4")) {
            //USER:"Place the order"
            //TTS:"OK. Pickup is ready in fifteen minutes. Would you like directions to REWE?"

            //TODO 展示购物下单完成UI（包含实际购买物品）
            AILog.d(TAG, "展示购物下单完成UI（包含实际购买物品）");

        } else if (task.equals("5")){
            //USER:"Sure"
            //TTS:"OK, I've set the destination for you"

            //TODO 设置 Breitscheidstraße 10, 70174 Stuttgart, Germany 为目的地
            AILog.d(TAG, "设置 Breitscheidstraße 10, 70174 Stuttgart, Germany 为目的地");

        } else if (task.equals("6")) {
            //TODO  唤醒
            //USER:"Let's go home"
            //TTS:"We don't have enough charge to make the trip. Shall we head to a charging station first?"
            //不用执行动作

        } else if (task.equals("7")) {
            //USER:"Sure"
            //TTS:"Here are the charging stations nearby. Which one shall we go to?"

            //TODO 展示附近充电站
            AILog.d(TAG, "展示附近充电站");
            
        } else if (task.equals("8")) {
            //USER:"The first one"
            //TTS:""We're on our way. Charger number three is available."

            //TODO 导航设置：
            //
            //途径点：
            //EnBW Charging Station
            //Talstraße 117, 70188 Stuttgart, Germany
            //
            //终点：
            //Dreisamstraße 3, 76337 Waldbronn, Germany
            AILog.d(TAG, "导航设置：\n" +
                    "\n" +
                    "途径点：\n" +
                    "EnBW Charging Station\n" +
                    "Talstraße 117, 70188 Stuttgart, Germany\n" +
                    "\n" +
                    "终点：\n" +
                    "Dreisamstraße 3, 76337 Waldbronn, Germany");
        }

    }

    /**
     * 场景3
     */
    public void SceneThree(String task) {
        if (task.equals("1")) {
            //USER:"""I have a meeting at City Plaza""
            //TTS:"There isn't enough charge for the trip. Shall we look for a charging station?"
            //不用执行动作

        }
        if (task.equals("2")) {
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

        } else if (task.equals("5")) {
            //USER:"""Reserve the first one for two hours""
            //TTS:"Got it. I've reserved parking from two thirty to four thirty PM. Updating directions."

            //TODO 5更新目的地为停车场
            AILog.d(TAG, "更新目的地为停车场");

        }
    }
}
