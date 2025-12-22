package com.innowise.paymentservice.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Component
@RequiredArgsConstructor
public class RandomOrgClient {

    private final RestTemplate restTemplate;

    @Value("${randomorg.api.url:https://www.random.org/integers/?num=1&min=1&max=100&col=1&base=10&format=plain}")
    private String randomOrgUrl;

    public Integer generateRandomNumber() {
        try {
            log.debug("Calling Random.org: {}", randomOrgUrl);

            String response = restTemplate.getForObject(randomOrgUrl, String.class);

            if (response == null || response.trim().isEmpty()) {
                log.error("Random.org returned empty response");
                throw new RestClientException("Random.org returned empty response");
            }

            Integer randomNumber = Integer.parseInt(response.trim());
            log.info("Random.org randomNumber: {}", randomNumber);

            return randomNumber;
        } catch (RestClientException e) {
            log.error("Failed to call Random.org: {}", e.getMessage());
            throw e;
        } catch (NumberFormatException e) {
            log.error("Failed to parse Random.org API response: {}", e.getMessage());
            throw new RestClientException("Invalid response format from Random.org API", e);
        }
    }

    public boolean isEven(Integer number) {
        return number % 2 == 0;
    }
}
