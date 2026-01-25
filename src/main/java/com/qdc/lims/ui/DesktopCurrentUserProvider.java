package com.qdc.lims.ui;

import com.qdc.lims.entity.User;
import org.springframework.stereotype.Component;

/**
 * Desktop implementation backed by SessionManager.
 */
@Component
public class DesktopCurrentUserProvider implements CurrentUserProvider {

    @Override
    public String getUsername() {
        User user = SessionManager.getCurrentUser();
        if (user == null) {
            return "UNKNOWN";
        }
        return user.getUsername() != null ? user.getUsername() : "UNKNOWN";
    }
}
