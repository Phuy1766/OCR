package vn.edu.congvan.signature.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record SignRequest(
        @NotNull UUID certificateId,
        @NotBlank String pkcs12Password,
        @Size(max = 500) String reason,
        @Size(max = 500) String location) {}
