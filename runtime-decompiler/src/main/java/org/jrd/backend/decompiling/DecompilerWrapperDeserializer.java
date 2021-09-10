package org.jrd.backend.decompiling;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

import java.lang.reflect.Type;
import java.util.LinkedList;
import java.util.List;

public class DecompilerWrapperDeserializer implements JsonDeserializer<DecompilerWrapper> {

    @Override
    public DecompilerWrapper deserialize(
            JsonElement jsonElement, Type type, JsonDeserializationContext jsonDeserializationContext
    ) throws JsonParseException {
        final JsonObject json = jsonElement.getAsJsonObject();

        final String name = json.get("Name").getAsString();
        final String wrapperURL = json.get("WrapperURL").getAsString();

        final List<String> dependencyURLs = new LinkedList<>();
        json.get("DependencyURL").getAsJsonArray().forEach(dependency -> dependencyURLs.add(dependency.getAsString()));

        final String decompilerURL = json.get("DecompilerDownloadURL").getAsString();

        return new DecompilerWrapper(name, wrapperURL, dependencyURLs, decompilerURL);
    }
}
