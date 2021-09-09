package org.jrd.backend.decompiling;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import java.lang.reflect.Type;

public class DecompilerWrapperInformationSerializer implements JsonSerializer<DecompilerWrapperInformation> {

    @Override
    public JsonElement serialize(DecompilerWrapperInformation decompilerWrapperInformation, Type type, JsonSerializationContext jsonSerializationContext) {

        final JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("Name", decompilerWrapperInformation.getName());
        jsonObject.addProperty("WrapperURL", decompilerWrapperInformation.getWrapperUrl().getRawUrl());
        final JsonArray jsonArray = new JsonArray();
        decompilerWrapperInformation.getDependencyUrls().forEach(url -> {
            jsonArray.add(url.getRawUrl());
        });
        jsonObject.add("DependencyURL", jsonArray);
        if (decompilerWrapperInformation.getDecompilerDownloadUrl() == null) {
            jsonObject.addProperty("DecompilerDownloadURL", "");
        } else {
            jsonObject.addProperty("DecompilerDownloadURL", decompilerWrapperInformation.getDecompilerDownloadUrl().toString());
        }
        return jsonObject;
    }
}
