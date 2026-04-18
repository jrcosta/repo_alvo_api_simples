package com.repoalvo.javaapi.service;

import com.repoalvo.javaapi.model.AgeEstimateResponse;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class ExternalService {

    private static final String AGIFY_URL = "https://api.agify.io";
    private final RestClient restClient;

    public ExternalService() {
        this.restClient = RestClient.builder().baseUrl(AGIFY_URL).build();
    }

    public AgeEstimateResponse estimateAge(String name) {
        try {
            AgeEstimateResponse response = restClient.get()
                    .uri(uriBuilder -> uriBuilder.queryParam("name", name).build())
                    .retrieve()
                    .body(AgeEstimateResponse.class);

            if (response == null) {
                return new AgeEstimateResponse(name, null, null);
            }

            return response;
        } catch (Exception e) {
            return new AgeEstimateResponse(name, null, null);
        }
    }
}
