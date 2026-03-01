package com.yatrika.community.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CommentRequestDTO {
    @NotBlank(message = "Comment cannot be empty")
    @Size(max = 500, message = "Comment is too long")
    private String content;
}
