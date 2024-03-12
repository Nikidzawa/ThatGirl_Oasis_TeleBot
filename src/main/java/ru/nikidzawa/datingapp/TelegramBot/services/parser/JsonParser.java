package ru.nikidzawa.datingapp.TelegramBot.services.parser;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import org.springframework.stereotype.Component;


@Component
public class JsonParser {
    @SneakyThrows
    public String getName(String json) {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        NameCity place = objectMapper.readValue(json, NameCity.class);
        return place.getCity();
    }

    public Geocode parseGeocode(String jsonString) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode rootNode = objectMapper.readTree(jsonString);
            JsonNode featureMemberNode = rootNode.path("response")
                    .path("GeoObjectCollection")
                    .path("featureMember");

            JsonNode firstGeoObjectNode = featureMemberNode.get(0).path("GeoObject");
            JsonNode pointNode = firstGeoObjectNode.path("Point");
            String pos = pointNode.path("pos").asText();
            String[] lonLat = pos.split(" ");
            Geocode geocode = new Geocode();
            geocode.setLon(Double.parseDouble(lonLat[1]));
            geocode.setLat(Double.parseDouble(lonLat[0]));
            return geocode;
        } catch (Exception e) {
            return null;
        }
    }
}

