package com.qdc.lims.ui;

/**
 * Provides the current username for audit stamping.
 *
 * In the desktop app there is no Spring Security authentication context,
 * so services should not depend on SecurityContextHolder.
 */
public interface CurrentUserProvider {
    String getUsername();
}
