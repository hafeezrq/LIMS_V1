package com.qdc.lims.service;

import com.qdc.lims.ui.navigation.DashboardType;
import javafx.stage.Stage;
import javafx.stage.Window;
import org.springframework.stereotype.Service;

import java.time.Year;

/**
 * Centralizes whitelabel branding and lab profile information.
 * <p>
 * This service reads configuration values and exposes consistent helpers for
 * window titles, report headers, and lab contact details.
 */
@Service
public class BrandingService {

    private static final String STAGE_CONTEXT_KEY = "branding.context";

    private final ConfigService configService;

    /**
     * Creates the branding service.
     *
     * @param configService configuration service
     */
    public BrandingService(ConfigService configService) {
        this.configService = configService;
    }

    /**
     * @return the application name (for example, "QDC LIMS")
     */
    public String getApplicationName() {
        String appName = configService.getTrimmed("APP_NAME", "QDC LIMS");
        return appName.isBlank() ? "QDC LIMS" : appName;
    }

    /**
     * @return the configured lab/clinic name, or an empty string if not set
     */
    public String getLabNameRaw() {
        return configService.getTrimmed("CLINIC_NAME", "");
    }

    /**
     * @return the lab name if present, otherwise the application name
     */
    public String getLabNameOrAppName() {
        String labName = getLabNameRaw();
        return labName.isBlank() ? getApplicationName() : labName;
    }

    /**
     * @return the configured clinic address (may be blank)
     */
    public String getClinicAddress() {
        return configService.getTrimmed("CLINIC_ADDRESS", "");
    }

    /**
     * @return the configured clinic phone (may be blank)
     */
    public String getClinicPhone() {
        return configService.getTrimmed("CLINIC_PHONE", "");
    }

    /**
     * @return the configured clinic email (may be blank)
     */
    public String getClinicEmail() {
        return configService.getTrimmed("CLINIC_EMAIL", "");
    }

    /**
     * Formats a window title using the lab name as a prefix.
     *
     * @param context a short context label (for example, "Admin Dashboard")
     * @return formatted window title
     */
    public String formatWindowTitle(String context) {
        String prefix = getLabNameOrAppName();
        if (context == null || context.isBlank()) {
            return prefix;
        }
        if (context.equals(prefix)) {
            return prefix;
        }
        return prefix + " - " + context;
    }

    /**
     * Formats a dashboard title using the dashboard's window title context.
     *
     * @param dashboardType dashboard type
     * @return formatted title
     */
    public String formatDashboardTitle(DashboardType dashboardType) {
        if (dashboardType == null) {
            return formatWindowTitle(getApplicationName());
        }
        return formatWindowTitle(dashboardType.getWindowTitle());
    }

    /**
     * Tags a stage with a branding context and applies a branded title.
     *
     * @param stage   stage to tag
     * @param context context label (for example, "System Configuration")
     */
    public void tagStage(Stage stage, String context) {
        if (stage == null) {
            return;
        }
        stage.getProperties().put(STAGE_CONTEXT_KEY, context);
        applyTaggedStageTitle(stage);
    }

    /**
     * Reapplies branded titles to all stages that have been tagged.
     */
    public void refreshAllTaggedStageTitles() {
        for (Window window : Window.getWindows()) {
            if (window instanceof Stage stage) {
                Object context = stage.getProperties().get(STAGE_CONTEXT_KEY);
                if (context instanceof String ctx && !ctx.isBlank()) {
                    stage.setTitle(formatWindowTitle(ctx));
                }
            }
        }
    }

    /**
     * @return whether the lab profile has been completed
     */
    public boolean isLabProfileComplete() {
        return configService.isLabProfileComplete();
    }

    /**
     * @return a branded report header string
     */
    public String getReportHeaderText() {
        String configured = configService.getTrimmed("REPORT_HEADER_TEXT", "");
        if (!configured.isBlank()) {
            return configured;
        }
        String labName = getLabNameRaw();
        if (!labName.isBlank()) {
            return labName + " Laboratory Report";
        }
        return getApplicationName() + " Report";
    }

    /**
     * @return configured report logo path (may be blank)
     */
    public String getReportLogoPath() {
        return configService.getTrimmed("REPORT_LOGO_PATH", "");
    }

    /**
     * @return a branded report footer string
     */
    public String getReportFooterText() {
        String configured = configService.getTrimmed("REPORT_FOOTER_TEXT", "");
        if (!configured.isBlank()) {
            return configured;
        }
        return "System Generated Report";
    }

    /**
     * @return a copyright line using the current year and lab name
     */
    public String getCopyrightLine() {
        int year = Year.now().getValue();
        return "\u00A9 " + year + " " + getLabNameOrAppName();
    }

    private void applyTaggedStageTitle(Stage stage) {
        Object context = stage.getProperties().get(STAGE_CONTEXT_KEY);
        if (context instanceof String ctx) {
            stage.setTitle(formatWindowTitle(ctx));
        }
    }
}
