package com.yatrika.subscription.dto.request;

import lombok.Builder;
import lombok.Data;

@Data @Builder
public class KhaltiInitRequest {
    private String return_url;
    private String website_url;
    private Long amount; // Paisa
    private String purchase_order_id;
    private String purchase_order_name;
}