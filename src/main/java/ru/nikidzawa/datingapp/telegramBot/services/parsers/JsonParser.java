package ru.nikidzawa.datingapp.telegramBot.services.parsers;

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
            Geocode geocode = new Geocode();
            geocode.setLat(55.7535926);
            geocode.setLon(37.62148935239179);
            return geocode;
        }
    }
    @SneakyThrows
    public String getHref (String json) {
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode jsonNode = objectMapper.readTree(json);
        return jsonNode.get("href").asText();
    }
}

