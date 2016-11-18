package com.wallpaper.bingfotor.utils;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.json.JSONObject;

/**
 * Created by Limuyang on 2016/11/18.
 */

public class HttpUtils {
    public static JsonObject getResposeJsonObject(JSONObject object) {
        String data = object.toString();
        JsonParser parser = new JsonParser();
        JsonElement elemen = parser.parse(data);
        return elemen.getAsJsonObject();

    }
}
