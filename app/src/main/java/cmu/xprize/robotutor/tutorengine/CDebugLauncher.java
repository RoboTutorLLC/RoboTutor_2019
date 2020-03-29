package cmu.xprize.robotutor.tutorengine;


import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

// if /sdcard/Download/debug.json exists
// bypass the activity selector and directly launch the tutor
public class CDebugLauncher {
    String intent;
    String intentData;
    String dataSource;
    String tutorId;
    String matrix;

    public Boolean launchIfDebug() {
        try {
            InputStream inputStream = new FileInputStream("/sdcard/Download/debug.json");

            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"), 8);
            StringBuilder sb = new StringBuilder();

            String line = null;
            while ((line = reader.readLine()) != null) {
                sb.append(line + "\n");
            }
            String result = sb.toString();

            Gson gson = new Gson();
            Map<String, String> mResult = gson.fromJson(result,
                    new TypeToken<HashMap<String, String>>() {}.getType());

            Log.wtf("CDebugLauncher", "content: "+ mResult.toString());

            this.intent = mResult.get("tutor_desc");
            this.intentData = "native";
            this.dataSource = mResult.get("tutor_data");
            this.tutorId = mResult.get("tutor_id");
            this.matrix = mResult.get("skill1");

            return true;
        } catch (Exception e) {
            Log.wtf("CDebugLauncher", "/sdcard/Download/debug.json does not exist");

            return false;
        }
    }

    public String getIntent(){
        return this.intent;
    }

    public String getIntentData() {
        return this.intentData;
    }

    public String getDataSource(){
        return this.dataSource;
    }

    public String getTutorId() {
        return this.tutorId;
    }

    public String getMatrix() {
        return this.matrix;
    }
}
