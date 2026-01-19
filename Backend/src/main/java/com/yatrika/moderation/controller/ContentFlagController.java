// com.yatrika.moderation.controller.ContentFlagController.java
package com.yatrika.moderation.controller;

import com.yatrika.moderation.domain.ContentFlag;
import com.yatrika.moderation.dto.FlagContentRequest;
import com.yatrika.moderation.service.ModerationService;
import com.yatrika.user.domain.User;
import com.yatrika.user.service.CurrentUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/flags")
@RequiredArgsConstructor
@Tag(name = "Content Flagging", description = "Endpoints for flagging inappropriate content")
public class ContentFlagController {
    private final ModerationService moderationService;
    private final CurrentUserService currentUserService;

    @Operation(summary = "Flag content as inappropriate")
    @PostMapping
    public ResponseEntity<Void> flagContent(@Valid @RequestBody FlagContentRequest request) {
        User reporter = currentUserService.getCurrentUserEntity();
        moderationService.flagContent(
                request.getContentType(),
                request.getContentId(),
                request.getReason(),
                reporter
        );
        return ResponseEntity.ok().build();
    }
}