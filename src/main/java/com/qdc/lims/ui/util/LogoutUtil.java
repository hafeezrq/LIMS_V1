package com.qdc.lims.ui.util;

import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.BorderPane;

import java.util.Optional;

/**
 * UI helper for session-scoped logout actions that close the tab containing a
 * given node after user confirmation.
 */
public final class LogoutUtil {

    private LogoutUtil() {
    }

    /**
     * Locates the parent tab for the provided node, asks for confirmation, and
     * removes the tab if confirmed.
     *
     * @param contentNode any node contained within the tab to close
     * @return {@code true} if a tab was closed
     */
    public static boolean confirmAndCloseParentTab(Node contentNode) {
        if (contentNode == null || contentNode.getScene() == null) {
            showUnableToCloseAlert();
            return false;
        }

        Parent dashboardRoot = contentNode.getScene().getRoot();
        if (dashboardRoot instanceof BorderPane mainBorderPane) {
            Node center = mainBorderPane.getCenter();
            if (center instanceof TabPane tabPane) {
                for (Tab tab : tabPane.getTabs()) {
                    if (isDescendantOf(contentNode, tab.getContent()) || tab.getContent() == contentNode) {
                        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
                        confirm.setTitle("Logout");
                        confirm.setHeaderText("Logout from this session?");
                        confirm.setContentText("This will close the current session.");

                        Optional<ButtonType> result = confirm.showAndWait();
                        if (result.isPresent() && result.get() == ButtonType.OK) {
                            tabPane.getTabs().remove(tab);
                            return true;
                        }
                        return false;
                    }
                }
            }
        }

        showUnableToCloseAlert();
        return false;
    }

    /**
     * Shows a fallback message when the tab cannot be resolved.
     */
    private static void showUnableToCloseAlert() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Logout");
        alert.setHeaderText(null);
        alert.setContentText("Unable to close session. Please close the tab manually.");
        alert.showAndWait();
    }

    /**
     * Determines whether {@code node} is contained within {@code potentialParent}
     * by traversing the parent chain.
     *
     * @param node node to test
     * @param potentialParent potential ancestor
     * @return {@code true} if the node is equal to or a descendant of the parent
     */
    private static boolean isDescendantOf(Node node, Node potentialParent) {
        if (potentialParent == null || node == null) {
            return false;
        }
        Parent current = node.getParent();
        while (current != null) {
            if (current == potentialParent) {
                return true;
            }
            current = current.getParent();
        }
        return node == potentialParent;
    }
}
