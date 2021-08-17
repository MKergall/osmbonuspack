package org.osmdroid.bonuspack.sharing;

import com.google.gson.JsonObject;

public class Partner {
    public String name, url, kmlUrl;

    public Partner(JsonObject jPO) {
        name = jPO.get("name").getAsString();
        url = jPO.get("url").getAsString();
        kmlUrl = jPO.get("kml_url").getAsString();
    }
}
