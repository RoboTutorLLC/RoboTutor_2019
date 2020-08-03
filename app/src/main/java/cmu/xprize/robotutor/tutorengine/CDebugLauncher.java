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
    Integer next_node_times;

    // For content_creation_mode
    String storyDataPath;

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
            this.next_node_times = Integer.valueOf(mResult.get("next_node_times"));

            this.storyDataPath = mResult.get("story_data_path");
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

    public Integer getNext_node_times() {
        Integer t = this.next_node_times;
        this.next_node_times = 0;
        return t;
    }

        /*
    modify the datasource
     */
    public String hijackJsonData(String jsonData){
        // not in debug mod
        if(!this.launchIfDebug())
            return jsonData;

        Log.wtf("DebugLauncher", "next_node_times: " + next_node_times.toString());
        Log.wtf("DebugLauncher", "old jsonData: " + jsonData);

        String[] list_name_to_truncate = {"data", "dataSource"};
        for(String list_name: list_name_to_truncate){
            Log.wtf("DebugLauncher", "now truncating: " + list_name);

            // find fist "gen_responseSet": [ ...
            Integer start = jsonData.indexOf(list_name);
            if(start == -1){ // not found
                continue;
            }
            Log.wtf("DebugLauncher", "found at: "+ start.toString());
            start = jsonData.indexOf('[', start) + 1;

            // find corresponding ]
            Integer depth = 1, cur = start;
            while (depth >= 1){
                if (jsonData.charAt(cur) == ']'){
                    depth -= 1;
                } else if (jsonData.charAt(cur) == '['){
                    depth += 1;
                }
                cur += 1;
            }
            cur -= 1; // go back before [

            // get start middle and end part of json
            String newJsonData_middle = jsonData.substring(start, cur);
            String newJsonData_start = jsonData.substring(0, start);
            String newJsonData_end = jsonData.substring(cur);

            // truncate middle part
            cur = 0;
            Integer skip_times_ = this.next_node_times;
            while(skip_times_ > 0){
                depth = 0;

                while (true){
                    // Log.wtf("DebugLauncher", newJsonData_middle.substring(cur));
                    if (newJsonData_middle.charAt(cur) == ',' && depth == 0){
                        break;
                    }

                    if (newJsonData_middle.charAt(cur) == '{' ||
                            newJsonData_middle.charAt(cur) == '['){
                        depth += 1;
                    }

                    if (newJsonData_middle.charAt(cur) == '}' ||
                            newJsonData_middle.charAt(cur) == ']'){
                        depth -= 1;
                    }
                    cur += 1;
                }

                skip_times_ -= 1;
                cur += 1; // skip this comma
            }
            newJsonData_middle = newJsonData_middle.substring(cur);

            // apply modification to jsonData
            Log.wtf("DebugLauncher", "newJsonData_start: " + newJsonData_start);
            Log.wtf("DebugLauncher", "newJsonData_middle: " + newJsonData_middle);
            Log.wtf("DebugLauncher", "newJsonData_end: " + newJsonData_end);
            jsonData = newJsonData_start + newJsonData_middle + newJsonData_end;
        }

        Log.wtf("DebugLauncher", "new jsonData: " + jsonData);
        return jsonData;
    }

}
