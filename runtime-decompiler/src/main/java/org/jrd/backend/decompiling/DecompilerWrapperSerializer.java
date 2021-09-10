package org.jrd.backend.decompiling;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import java.lang.reflect.Type;

public class DecompilerWrapperSerializer implements JsonSerializer<DecompilerWrapper> {

    @Override
    public JsonElement serialize(
            DecompilerWrapper decompilerWrapper, Type type, JsonSerializationContext jsonSerializationContext
    ) {
        final JsonObject jsonObject = new JsonObject();

        jsonObject.addProperty("Name", decompilerWrapper.getName());
        jsonObject.addProperty("WrapperURL", decompilerWrapper.getWrapperUrl().getRawUrl());

        final JsonArray jsonArray = new JsonArray();
        decompilerWrapper.getDependencyUrls().forEach(url -> {
            jsonArray.add(url.getRawUrl());
        });

        jsonObject.add("DependencyURL", jsonArray);
        if (decompilerWrapper.getDecompilerDownloadUrl() == null) {
            jsonObject.addProperty("DecompilerDownloadURL", "");
        } else {
            jsonObject.addProperty("DecompilerDownloadURL", decompilerWrapper.getDecompilerDownloadUrl().toString());
        }

        return jsonObject;
    }
}
