package lk.ijse.etechbackend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

public class ForgotPasswordDTO {

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class RequestOtp {
        @NotBlank(message = "Username or email is required")
        private String identifier;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class VerifyOtp {
        @NotBlank(message = "Username or email is required")
        private String identifier;

        @NotBlank(message = "Verification code is required")
        @Size(min = 6, max = 6, message = "OTP must be exactly 6 digits")
        private String otp;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ResetPassword {
        @NotBlank(message = "Username or email is required")
        private String identifier;

        @NotBlank(message = "Reset token or OTP is required")
        private String resetToken;

        @NotBlank(message = "New password is required")
        @Size(min = 6, message = "Password must be at least 6 characters")
        private String newPassword;
    }
}
