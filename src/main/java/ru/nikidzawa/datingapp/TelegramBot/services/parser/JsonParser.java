package ru.nikidzawa.datingapp.TelegramBot.services.parser;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class JsonParser {
    @SneakyThrows
    public String getName(String json) {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        NameCity place = objectMapper.readValue(json, NameCity.class);
        return place.getCity();
    }

    @SneakyThrows
    public Geocode getGeocode (String geoObject) {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        List<Geocode> geocodes = objectMapper.readValue(geoObject, new TypeReference<List<Geocode>>() {});
        return geocodes.getFirst();
    }
}

