package ru.nikidzawa.datingapp.API;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class ExternalApi {
    String[] apiKeysPull = {"a9e37ef6-bfed-43a3-81eb-1f8cbd52bbc1", "6fcdd42f-151b-4068-b587-ffaec9a48512", "69b2e791-2b45-4268-841f-2a98c25e62cc"};
    int currentKey = 0;

    public ResponseEntity<?> createFolder (Long path) {
        String url = "https://cloud-api.yandex.net/v1/disk/resources?path=" + path;
        HttpEntity<String> requestEntity = new HttpEntity<>(null, getHeader());

        RestTemplate restTemplate = new RestTemplate();
        return restTemplate.exchange(
                url,
                HttpMethod.PUT,
                requestEntity,
                String.class
        );
    }

    public void uploadImage(String path, String url) {
        String postImageUrl = "https://cloud-api.yandex.net/v1/disk/resources/upload?path=" + path + "&url=" + url;
        HttpEntity<String> requestEntity = new HttpEntity<>(null, getHeader());

        RestTemplate restTemplate = new RestTemplate();
        restTemplate.exchange(
                postImageUrl,
                HttpMethod.POST,
                requestEntity,
                String.class
        );
    }

    public void deleteFolder (Long path) {
        String url = "https://cloud-api.yandex.net/v1/disk/resources?path=" + path;
        HttpEntity<String> requestEntity = new HttpEntity<>(null, getHeader());

        RestTemplate restTemplate = new RestTemplate();
        restTemplate.exchange(
                url,
                HttpMethod.DELETE,
                requestEntity,
                String.class
        );
    }

    public HttpHeaders getHeader () {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Content-Type", "application/json");
        headers.set("Authorization", "OAuth y0_AgAAAAA6DSoUAADLWwAAAAD-7B2bAAAmShKyx9pJQ7i_DbM850VcHQn9NA");
        return headers;
    }

    public String getCoordinates(String geoObject) {
        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<String> response;

        String url = "https://geocode-maps.yandex.ru/1.x?apikey=" + apiKeysPull[currentKey] + "&format=json&geocode=" + geoObject;
        response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                null,
                String.class
        );

        if (!response.getStatusCode().is2xxSuccessful()) {
            currentKey++;
            if (currentKey >= apiKeysPull.length) {
                currentKey = 0;
            }
            getCoordinates(geoObject);
        }

        return response.getBody();
    }

    public String getCityName(double latitude, double longitude) {
        String url = "https://api.bigdatacloud.net/data/reverse-geocode-client?latitude=" + latitude + "&longitude=" + longitude + "&localityLanguage=ru";
        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<String> response;
        response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                null,
                String.class
        );
        return response.getBody();
    }
}
