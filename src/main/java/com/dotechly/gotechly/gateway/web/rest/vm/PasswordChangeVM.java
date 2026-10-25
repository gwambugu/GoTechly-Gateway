package com.dotechly.gotechly.gateway.web.rest.vm;

import java.io.Serializable;

/**
 * View Model object for storing the user's current and new password.
 */
public class PasswordChangeVM implements Serializable {

    private static final long serialVersionUID = 1L;

    private String currentPassword;

    private String newPassword;

    public PasswordChangeVM() {
        // Empty constructor needed for Jackson.
    }

    public PasswordChangeVM(String currentPassword, String newPassword) {
        this.currentPassword = currentPassword;
        this.newPassword = newPassword;
    }

    public String getCurrentPassword() {
        return currentPassword;
    }

    public void setCurrentPassword(String currentPassword) {
        this.currentPassword = currentPassword;
    }

    public String getNewPassword() {
        return newPassword;
    }

    public void setNewPassword(String newPassword) {
        this.newPassword = newPassword;
    }
}
