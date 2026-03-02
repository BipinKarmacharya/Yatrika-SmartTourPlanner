package com.yatrika.notification.service;

import com.yatrika.notification.dto.response.WeatherResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class WeatherService {

    private final WebClient.Builder webClientBuilder;

    @Value("${yatrika.weather.api-key}")
    private String apiKey;

    @Value("${yatrika.weather.base-url}")
    private String baseUrl;

    /**
     * Fetches current weather for specific coordinates.
     */
    public WeatherResponse fetchWeather(BigDecimal lat, BigDecimal lon) {
        return webClientBuilder.build()
                .get()
                .uri(uriBuilder -> uriBuilder
                        .scheme("https")
                        .host("api.openweathermap.org")
                        .path("/data/2.5/weather")
                        .queryParam("lat", lat)
                        .queryParam("lon", lon)
                        .queryParam("appid", apiKey)
                        .queryParam("units", "metric") // Get temperature in Celsius
                        .build())
                .retrieve()
                .bodyToMono(WeatherResponse.class)
                .block(); // .block() is used here because the Scheduler is a background thread
    }
}