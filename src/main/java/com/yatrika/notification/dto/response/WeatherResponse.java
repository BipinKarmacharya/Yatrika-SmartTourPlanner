package com.yatrika.notification.dto.response;

import lombok.Data;
import java.util.List;

@Data
public class WeatherResponse {
    private List<Weather> weather;
    private Main main;
    private String name;

    @Data
    public static class Weather {
        private String main;        // e.g., "Rain", "Snow", "Clear"
        private String description; // e.g., "heavy intensity rain"
        private String icon;
    }

    @Data
    public static class Main {
        private Double temp;
        private Double feels_like;
    }
}