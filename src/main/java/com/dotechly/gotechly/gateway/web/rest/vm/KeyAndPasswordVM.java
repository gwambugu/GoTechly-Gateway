package com.dotechly.gotechly.gateway.web.rest.vm;

/**
 * View Model object for storing the user's key and new password.
 */
public class KeyAndPasswordVM {

    private String key;

    private String newPassword;

    public KeyAndPasswordVM() {
        // Empty constructor needed for Jackson.
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getNewPassword() {
        return newPassword;
    }

    public void setNewPassword(String newPassword) {
        this.newPassword = newPassword;
    }
}
