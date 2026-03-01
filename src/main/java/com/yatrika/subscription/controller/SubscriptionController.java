package com.yatrika.subscription.controller;

import com.yatrika.shared.security.UserPrincipal;
import com.yatrika.subscription.domain.Subscription;
import com.yatrika.subscription.dto.response.KhaltiInitResponse;
import com.yatrika.subscription.repository.SubscriptionRepository;
import com.yatrika.subscription.service.KhaltiService;
import com.yatrika.subscription.service.SubscriptionService;
import com.yatrika.user.domain.User;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/subscriptions")
@RequiredArgsConstructor
public class SubscriptionController {

    private final KhaltiService khaltiService;
    private final SubscriptionService subscriptionService;
    private final SubscriptionRepository subscriptionRepository;

    @GetMapping("/status")
    public ResponseEntity<Map<String, String>> getStatus(@AuthenticationPrincipal User user) {
        Subscription sub = subscriptionRepository.findByUserId(user.getId())
                .orElseThrow(() -> new RuntimeException("Subscription not found"));

        return ResponseEntity.ok(Map.of("tier", sub.getTier().name()));
    }

    @PostMapping("/initiate-upgrade")
    public ResponseEntity<KhaltiInitResponse> upgrade(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        if (principal == null) {
            throw new RuntimeException("Unauthenticated");
        }

        return ResponseEntity.ok(
                khaltiService.initiatePayment(principal.getId(), 10000L)
        );
    }

    // The WebView will hit this URL after Khalti payment
    @GetMapping("/payment-callback")
    public ResponseEntity<String> paymentCallback() {

        String html = """
        <html>
            <head>
                <title>Payment Complete</title>
                <style>
                    body { font-family: Arial, sans-serif; text-align: center; padding-top: 50px; }
                    h2 { color: #4CAF50; }
                </style>
            </head>
            <body>
                <h2>Payment Completed Successfully!</h2>
                <p>You can now close this window.</p>
            </body>
        </html>
        """;

        return ResponseEntity.ok().contentType(MediaType.TEXT_HTML).body(html);
    }

    @PostMapping("/verify")
    public ResponseEntity<?> verify(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam String pidx
    ) {
        boolean success = khaltiService.verifyPayment(pidx, principal.getId());

        if (success) {
            subscriptionService.upgradeToPro(principal.getId());
            return ResponseEntity.ok(
                    Map.of("message", "Welcome to PRO!", "status", "SUCCESS")
            );
        }

        return ResponseEntity.badRequest().body("Payment verification failed.");
    }
}