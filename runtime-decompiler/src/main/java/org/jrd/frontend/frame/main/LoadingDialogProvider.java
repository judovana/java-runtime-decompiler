package org.jrd.frontend.frame.main;

import java.awt.event.ActionListener;

public interface LoadingDialogProvider {
    default void showLoadingDialog(ActionListener action, String title) {
    }

    default void hideLoadingDialog() {
    }

}
