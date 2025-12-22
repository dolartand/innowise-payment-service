package com.innowise.paymentservice.client;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("RandomOrgClient Unit Tests")
class RandomOrgClientTest {

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private RandomOrgClient randomOrgClient;

    private static final String apiUrl = "https://www.random.org/integers/?num=1&min=1&max=100&col=1&base=10&format=plain";

    @Test
    @DisplayName("should generate random number successfully")
    void shouldGenerateRandomNumber_Successfully() {
        ReflectionTestUtils.setField(randomOrgClient, "randomOrgUrl", apiUrl);

        when(restTemplate.getForObject(apiUrl, String.class)).thenReturn("42");

        Integer result = randomOrgClient.generateRandomNumber();

        assertThat(result).isEqualTo(42);
        verify(restTemplate).getForObject(apiUrl, String.class);
    }

    @Test
    @DisplayName("should handle response with whitespace")
    void shouldHandleResponse_WithWhitespace() {
        ReflectionTestUtils.setField(randomOrgClient, "randomOrgUrl", apiUrl);

        when(restTemplate.getForObject(apiUrl, String.class)).thenReturn("  25  \n");

        Integer result = randomOrgClient.generateRandomNumber();

        assertThat(result).isEqualTo(25);
    }

    @Test
    @DisplayName("should throw RestClientException when API returns empty response")
    void shouldThrowException_WhenEmptyResponse() {
        ReflectionTestUtils.setField(randomOrgClient, "randomOrgUrl", apiUrl);

        when(restTemplate.getForObject(apiUrl, String.class)).thenReturn("");

        assertThatThrownBy(() -> randomOrgClient.generateRandomNumber())
                .isInstanceOf(RestClientException.class)
                .hasMessageContaining("empty response");
    }

    @Test
    @DisplayName("should throw RestClientException when API returns null")
    void shouldThrowException_WhenNullResponse() {
        ReflectionTestUtils.setField(randomOrgClient, "randomOrgUrl", apiUrl);

        when(restTemplate.getForObject(apiUrl, String.class)).thenReturn(null);

        assertThatThrownBy(() -> randomOrgClient.generateRandomNumber())
                .isInstanceOf(RestClientException.class)
                .hasMessageContaining("empty response");
    }

    @Test
    @DisplayName("should throw RestClientException when response is not a number")
    void shouldThrowException_WhenInvalidNumberFormat() {
        ReflectionTestUtils.setField(randomOrgClient, "randomOrgUrl", apiUrl);

        when(restTemplate.getForObject(apiUrl, String.class)).thenReturn("not-a-number");

        assertThatThrownBy(() -> randomOrgClient.generateRandomNumber())
                .isInstanceOf(RestClientException.class)
                .hasMessageContaining("Invalid response format");
    }

    @Test
    @DisplayName("should throw RestClientException when API call fails")
    void shouldThrowException_WhenApiCallFails() {
        ReflectionTestUtils.setField(randomOrgClient, "randomOrgUrl", apiUrl);

        when(restTemplate.getForObject(apiUrl, String.class))
                .thenThrow(new RestClientException("Connection timeout"));

        assertThatThrownBy(() -> randomOrgClient.generateRandomNumber())
                .isInstanceOf(RestClientException.class)
                .hasMessageContaining("Connection timeout");
    }

    @Test
    @DisplayName("should return true for even numbers")
    void shouldReturnTrue_ForEvenNumbers() {
        assertThat(randomOrgClient.isEven(2)).isTrue();
        assertThat(randomOrgClient.isEven(4)).isTrue();
        assertThat(randomOrgClient.isEven(100)).isTrue();
        assertThat(randomOrgClient.isEven(0)).isTrue();
        assertThat(randomOrgClient.isEven(-2)).isTrue();
    }

    @Test
    @DisplayName("should return false for odd numbers")
    void shouldReturnFalse_ForOddNumbers() {
        assertThat(randomOrgClient.isEven(1)).isFalse();
        assertThat(randomOrgClient.isEven(3)).isFalse();
        assertThat(randomOrgClient.isEven(99)).isFalse();
        assertThat(randomOrgClient.isEven(-1)).isFalse();
    }
}