package com.yatrika.shared.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yatrika.destination.dto.request.DestinationRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class StringToDestinationRequestConverter implements Converter<String, DestinationRequest> {

    private final ObjectMapper objectMapper;

    @Override
    public DestinationRequest convert(String source) {
        try {
            return objectMapper.readValue(source, DestinationRequest.class);
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to convert JSON string to DestinationRequest", e);
        }
    }
}