package ru.nikidzawa.datingapp.API;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class ExternalApi {
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
}
