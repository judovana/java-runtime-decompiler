package org.jrd.backend.decompiling;

import com.google.gson.*;

import java.lang.reflect.Type;

public class DecompilerWrapperInformationSerializer implements JsonSerializer<DecompilerWrapperInformation> {

    @Override
    public JsonElement serialize(DecompilerWrapperInformation decompilerWrapperInformation, Type type, JsonSerializationContext jsonSerializationContext) {

        final JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("Name", decompilerWrapperInformation.getName());
        jsonObject.addProperty("WrapperURL", decompilerWrapperInformation.getWrapperURL().toString());
        final JsonArray jsonArray = new JsonArray();
        decompilerWrapperInformation.getDependencyURLs().forEach(url -> {
            jsonArray.add(url.getPath());
        });
        jsonObject.add("DependencyURL", jsonArray);
        jsonObject.addProperty("DecompilerDownloadURL", decompilerWrapperInformation.getDecompilerURL().toString());
        return jsonObject;
    }
}