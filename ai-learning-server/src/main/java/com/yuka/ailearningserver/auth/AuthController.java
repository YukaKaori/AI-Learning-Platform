package com.yuka.ailearningserver.auth;

import com.yuka.ailearningserver.auth.dto.AuthUserResponse;
import com.yuka.ailearningserver.auth.dto.LoginRequest;
import com.yuka.ailearningserver.auth.dto.LoginResponse;
import com.yuka.ailearningserver.auth.dto.LogoutRequest;
import com.yuka.ailearningserver.auth.dto.RefreshRequest;
import com.yuka.ailearningserver.auth.dto.TokenPairResponse;
import com.yuka.ailearningserver.auth.dto.UpdateProfileRequest;
import com.yuka.ailearningserver.auth.security.AuthenticatedUser;
import com.yuka.ailearningserver.auth.token.TokenService.ClientInfo;
import com.yuka.ailearningserver.common.api.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private static final int MAX_USER_AGENT_LENGTH = 255;

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request,
                                            HttpServletRequest httpRequest) {
        return ApiResponse.success(authService.login(request, clientInfo(httpRequest)));
    }

    @PostMapping("/refresh")
    public ApiResponse<TokenPairResponse> refresh(@Valid @RequestBody RefreshRequest request,
                                                  HttpServletRequest httpRequest) {
        return ApiResponse.success(authService.refresh(request.refreshToken(), clientInfo(httpRequest)));
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout(@Valid @RequestBody LogoutRequest request) {
        authService.logout(request.refreshToken());
        return ApiResponse.success();
    }

    @GetMapping("/me")
    public ApiResponse<AuthUserResponse> me(@AuthenticationPrincipal AuthenticatedUser principal) {
        return ApiResponse.success(authService.currentUser(principal));
    }

    @PutMapping("/profile")
    public ApiResponse<AuthUserResponse> updateProfile(@AuthenticationPrincipal AuthenticatedUser principal,
                                                       @Valid @RequestBody UpdateProfileRequest request) {
        return ApiResponse.success(authService.updateProfile(principal.id(), request));
    }

    /**
     * Client metadata for auditing. Trusts the first {@code X-Forwarded-For} hop —
     * accurate once behind our own reverse proxy; audit-only, never authorization.
     */
    private static ClientInfo clientInfo(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        String ip = forwarded != null && !forwarded.isBlank()
                ? forwarded.split(",")[0].trim()
                : request.getRemoteAddr();
        String userAgent = request.getHeader(HttpHeaders.USER_AGENT);
        if (userAgent != null && userAgent.length() > MAX_USER_AGENT_LENGTH) {
            userAgent = userAgent.substring(0, MAX_USER_AGENT_LENGTH);
        }
        return new ClientInfo(ip, userAgent);
    }
}
