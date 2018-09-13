package com.redhat.thermostat.vm.decompiler.decompiling;

import com.google.gson.*;

import java.lang.reflect.Type;
import java.util.LinkedList;
import java.util.List;

public class DecompilerWrapperInformationDeserializer implements JsonDeserializer<DecompilerWrapperInformation> {

    @Override
    public DecompilerWrapperInformation deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {

        final JsonObject json = jsonElement.getAsJsonObject();

        final String name = json.get("Name").getAsString();
        final String fullyQualifiedClassName = json.get("FullyQualifiedClassName").getAsString();
        final String wrapperURL = json.get("WrapperURL").getAsString();
        final List<String> dependencyURLs = new LinkedList<>();
        json.get("DependencyURL").getAsJsonArray().forEach(dependency -> dependencyURLs.add(dependency.getAsString()));

        return new DecompilerWrapperInformation(name, fullyQualifiedClassName, wrapperURL, dependencyURLs);
    }
}
