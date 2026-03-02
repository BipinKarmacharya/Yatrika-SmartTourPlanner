package com.yatrika.subscription.service;

import com.yatrika.subscription.dto.request.KhaltiInitRequest;
import com.yatrika.subscription.dto.response.KhaltiInitResponse;
import com.yatrika.subscription.repository.SubscriptionRepository;
import com.yatrika.subscription.repository.TransactionRepository;
import com.yatrika.user.domain.User;
import com.yatrika.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import com.yatrika.subscription.domain.Transaction;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class KhaltiService {

    private final WebClient khaltiWebClient;
    private final SubscriptionRepository subscriptionRepository;
    private final TransactionRepository transactionRepository;

    private final UserRepository userRepository;

    // Update initiatePayment to ensure the User reference is persistent
    public KhaltiInitResponse initiatePayment(Long userId, Long amountPaisa) {
        String orderId = "ORDER_" + System.currentTimeMillis();
        String ngrokBaseUrl = "https://vanadic-semipostal-cody.ngrok-free.dev";

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        KhaltiInitRequest request = KhaltiInitRequest.builder()
                // IMPORTANT: This must match your backend URL for the WebView to catch it
                .return_url(ngrokBaseUrl + "/api/v1/subscriptions/payment-callback")
                .website_url(ngrokBaseUrl)
                .amount(amountPaisa)
                .purchase_order_id(orderId)
                .purchase_order_name("Yatrika Pro Subscription")
                .build();

        KhaltiInitResponse response = khaltiWebClient.post()
                .uri("/epayment/initiate/")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(KhaltiInitResponse.class)
                .block();

        if (response != null) {
            // Save transaction with explicit user link
            Transaction transaction = Transaction.builder()
                    .user(user)
                    .pidx(response.getPidx())
                    .amount(amountPaisa)
                    .purchaseOrderId(orderId)
                    .status("PENDING")
                    .build();
            transactionRepository.save(transaction);
        }

        return response;
    }

    public boolean verifyPayment(String pidx, Long userId) {

        Transaction transaction = transactionRepository.findByPidx(pidx)
                .orElseThrow(() -> new RuntimeException("Transaction not found"));

        if ("COMPLETED".equals(transaction.getStatus())) {
            throw new IllegalStateException("Payment already verified");
        }

        if (!transaction.getUser().getId().equals(userId)) {
            throw new AccessDeniedException("Not your transaction");
        }

        // 2. Call Khalti lookup API
        Map<String, Object> response = khaltiWebClient.post()
                .uri("/epayment/lookup/")
                .bodyValue(Map.of("pidx", pidx))
                .retrieve()
                .bodyToMono(Map.class)
                .block();

        if (response != null && "Completed".equals(response.get("status"))) {
            transaction.setStatus("COMPLETED");
            transactionRepository.save(transaction);
            return true;
        }

        return false;
    }
}
