package org.osmdroid.bonuspack.sharing;

import android.util.Log;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import org.osmdroid.bonuspack.utils.BonusPackHelper;
import org.osmdroid.util.GeoPoint;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;

/**
 * Friends sharing API
 * @author M.Kergall
 */
public class Friends {
    public ArrayList<Friend> friendsList;
    public ArrayList<Partner> partners;
    public static final String NAV_SERVER_URL = "https://comob.org/sharing/";
    protected String mUserAgent;

    public Friends(String userAgent){
        mUserAgent = userAgent;
    }

    public Friend get(int index){
        return friendsList.get(index);
    }

    public int size(){
        return friendsList.size();
    }

    /**
     * @param friendId
     * @return the index of the friend with id. -1 if not found.
     */
    public int getFriendWithId(String friendId){
        if (friendId == null || friendsList == null)
            return -1;
        for (int i=0; i<size(); i++) {
            Friend f = get(i);
            if (friendId.equals(f.mId))
                return i;
        }
        return -1;
    }

    public String callStartSharing(String uniqueId, String nickname, String group, String message) {
        //List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(4);
        String url = null;
        try {
            url = NAV_SERVER_URL + "jstart.php?"
                    + "nickname=" + URLEncoder.encode(nickname, "UTF-8")
                    + "&group_id=" + URLEncoder.encode(group, "UTF-8")
                    + "&user_id=" + URLEncoder.encode(uniqueId, "UTF-8")
                    + "&message=" + URLEncoder.encode(message, "UTF-8");
        } catch (UnsupportedEncodingException e) {
        }
        /*
		nameValuePairs.add(new BasicNameValuePair("nickname", nickname));
		nameValuePairs.add(new BasicNameValuePair("group_id", group));
		nameValuePairs.add(new BasicNameValuePair("user_id", getUniqueId()));
		nameValuePairs.add(new BasicNameValuePair("message", message));
		String result = BonusPackHelper.requestStringFromPost(url, nameValuePairs);
		*/
        String result = BonusPackHelper.requestStringFromUrl(url, mUserAgent);
        if (result == null) {
            return "Technical error with the server";
        }
        try {
            JsonElement json = JsonParser.parseString(result);
            JsonObject jResult = json.getAsJsonObject();
            String answer = jResult.get("answer").getAsString();
            if (!"ok".equals(answer)) {
                return jResult.get("error").getAsString();
            }
            JsonArray jPartners = jResult.get("partners").getAsJsonArray();
            partners = new ArrayList(jPartners.size());
            for (JsonElement jPartner:jPartners){
                JsonObject jPO = jPartner.getAsJsonObject();
                Partner partner = new Partner(jPO);
                partners.add(partner);
            }
        } catch (JsonSyntaxException e) {
            return "Technical error with the server";
        }
        return null;
    }

    public String callUpdateSharing(String uniqueId, GeoPoint myPosition, double azimuthAngleSpeed) {
        friendsList = null;
        int hasLocation = (myPosition != null ? 1 : 0);
        if (myPosition == null)
            myPosition = new GeoPoint(0.0, 0.0);
        String url = null;
        try {
            url = NAV_SERVER_URL + "jupdate.php?"
                    + "user_id=" + URLEncoder.encode(uniqueId, "UTF-8")
                    + "&has_location=" + hasLocation
                    + "&lat=" + myPosition.getLatitude()
                    + "&lon=" + myPosition.getLongitude()
                    + "&bearing=" + azimuthAngleSpeed;
        } catch (UnsupportedEncodingException e) {
            return "Technical error with the server";
        }
        Log.d(BonusPackHelper.LOG_TAG, "callUpdateSharing:" + url);
        String result = BonusPackHelper.requestStringFromUrl(url, mUserAgent);
        if (result == null) {
            return "Technical error with the server";
        }
        try {
            JsonElement json = JsonParser.parseString(result);
            JsonObject jResult = json.getAsJsonObject();
            String answer = jResult.get("answer").getAsString();
            if (!"ok".equals(answer)) {
                return jResult.get("error").getAsString();
            }
            JsonArray jFriends = jResult.get("people").getAsJsonArray();
            friendsList = new ArrayList<Friend>(jFriends.size());
            for (JsonElement jFriend : jFriends) {
                JsonObject joFriend = (JsonObject) jFriend;
                Friend friend = new Friend(joFriend);
                friendsList.add(friend);
            }
        } catch (JsonSyntaxException e) {
            return "Technical error with the server";
        }
        return null;
    }

    public String callStopSharing(String uniqueId) {
        friendsList = null;
        String url = null;
        try {
            url = NAV_SERVER_URL + "jstop.php?"
                    + "user_id=" + URLEncoder.encode(uniqueId, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            return "Technical error with the server";
        }
        String result = BonusPackHelper.requestStringFromUrl(url, mUserAgent);
        if (result == null) {
            return "Technical error with the server";
        }
        try {
            JsonElement json = JsonParser.parseString(result);
            JsonObject jResult = json.getAsJsonObject();
            String answer = jResult.get("answer").getAsString();
            if (!"ok".equals(answer)) {
                return jResult.get("error").getAsString();
            }
        } catch (JsonSyntaxException e) {
            return "Technical error with the server";
        }
        return null;
    }
}

