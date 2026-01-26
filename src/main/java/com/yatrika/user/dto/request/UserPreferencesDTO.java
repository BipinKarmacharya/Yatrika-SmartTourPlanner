package com.yatrika.user.dto.request;

import lombok.Data;

import java.util.List;

@Data
public class UserPreferencesDTO {
    private List<String> interests;
}