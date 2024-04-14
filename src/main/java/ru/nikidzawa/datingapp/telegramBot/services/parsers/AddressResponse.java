package ru.nikidzawa.datingapp.telegramBot.services.parsers;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class AddressResponse {

    @JsonProperty("address")
    private Address address;

    @Getter
    @Setter
    public static class Address {
        @JsonProperty("city")
        private String city;

        @JsonProperty("state")
        private String state;

        @JsonProperty("country")
        private String country;

        @JsonProperty("municipality")
        private String municipality;

        @JsonProperty("town")
        private String town;

        @JsonProperty("village")
        private String village;
    }
}
