package main;

import com.google.gson.*;
import com.grum.geocalc.Coordinate;
import com.grum.geocalc.Point;

import java.lang.reflect.Type;


/**
 * Используется для десериализации термограммы.
 */
public class ThermogramDeserializer implements JsonDeserializer<Thermogram> {
    @Override
    public Thermogram deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) {
        JsonObject jsonObject = json.getAsJsonObject();
        double yaw = jsonObject.get("GimbalYawDegree").getAsDouble();
        double height = jsonObject.get("RelativeAltitude").getAsDouble();
        double latitude = jsonObject.get("GPSLatitude").getAsDouble();
        double longitude = jsonObject.get("GPSLongitude").getAsDouble();
        return new Thermogram(yaw, height, Point.at(Coordinate.fromDegrees(latitude), Coordinate.fromDegrees(longitude)));
    }
}