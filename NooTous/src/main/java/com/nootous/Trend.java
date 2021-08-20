package com.nootous;

import android.util.Log;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import org.osmdroid.bonuspack.sharing.Friends;
import org.osmdroid.bonuspack.utils.BonusPackHelper;
import java.util.List;

public class Trend {
    public String name;
    public int count;
    public Trend(JsonObject jPO) {
        name = jPO.get("group_id").getAsString();
        count = jPO.get("group_count").getAsInt();
    }
    public String displayedCount(){
        if (count < 10)
            return String.valueOf(count);
        else if (count < 100)
            return ">10";
        else if (count < 1000)
            return ">100";
        else if (count < 100000)
            return String.valueOf(count/1000)+"K";
        else
            return String.valueOf(count/1000000)+"M";
    }

    public static String getTrends(List<Trend> trends) {
        String url = Friends.NAV_SERVER_URL + "jtrends.php";
        String result = BonusPackHelper.requestStringFromUrl(url);
        if (result == null) {
            return "Technical error with the server";
        }
        Log.d(BonusPackHelper.LOG_TAG, "getTrends:" + url);
        try {
            JsonElement json = JsonParser.parseString(result);
            JsonObject jResult = json.getAsJsonObject();
            String answer = jResult.get("answer").getAsString();
            if (!"ok".equals(answer)) {
                return jResult.get("error").getAsString();
            }
            JsonArray jTrends = jResult.get("trends").getAsJsonArray();
            trends.clear();
            for (JsonElement jPartner:jTrends){
                JsonObject jPO = jPartner.getAsJsonObject();
                Trend trend = new Trend(jPO);
                trends.add(trend);
            }
        } catch (JsonSyntaxException e) {
            return "Technical error with the server";
        }
        return null;
    }
}
