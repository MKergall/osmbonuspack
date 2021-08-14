package org.osmdroid.bonuspack.sharing;

import com.google.gson.JsonObject;

public class Partner {
    public String name, url, kml_url;

    public Partner(JsonObject jPO) {
        name = jPO.get("name").getAsString();
        url = jPO.get("url").getAsString();
        kml_url = jPO.get("kml_url").getAsString();
    }
}
