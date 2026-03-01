package com.yatrika.subscription.dto.response;

import lombok.Data;

@Data
public class KhaltiInitResponse {
    private String pidx;
    private String payment_url;
    private String expires_at;
}
