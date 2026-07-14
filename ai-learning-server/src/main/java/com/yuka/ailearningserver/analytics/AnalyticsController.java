package com.yuka.ailearningserver.analytics;

import com.yuka.ailearningserver.analytics.dto.ActivityDayResponse;
import com.yuka.ailearningserver.analytics.dto.AnalyticsSummaryResponse;
import com.yuka.ailearningserver.analytics.dto.SubjectShareResponse;
import com.yuka.ailearningserver.auth.security.AuthenticatedUser;
import com.yuka.ailearningserver.common.api.ApiResponse;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/analytics")
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    public AnalyticsController(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    @GetMapping("/summary")
    public ApiResponse<AnalyticsSummaryResponse> summary(@AuthenticationPrincipal AuthenticatedUser principal) {
        return ApiResponse.success(analyticsService.summary(principal.id()));
    }

    /** {@code days} is capped at 90 — the heatmap fetches 84, the bar chart less. */
    @GetMapping("/activity")
    public ApiResponse<List<ActivityDayResponse>> activity(@AuthenticationPrincipal AuthenticatedUser principal,
                                                           @RequestParam(defaultValue = "30") int days) {
        return ApiResponse.success(analyticsService.activity(principal.id(), days));
    }

    @GetMapping("/subject-shares")
    public ApiResponse<List<SubjectShareResponse>> subjectShares(@AuthenticationPrincipal AuthenticatedUser principal,
                                                                 @RequestParam(defaultValue = "30") int days) {
        return ApiResponse.success(analyticsService.subjectShares(principal.id(), days));
    }
}
