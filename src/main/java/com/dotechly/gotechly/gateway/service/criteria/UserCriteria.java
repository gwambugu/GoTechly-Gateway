package com.dotechly.gotechly.gateway.service.criteria;

import java.io.Serializable;
import java.util.Objects;

/**
 * Criteria class for the {@link com.dotechly.gotechly.gateway.domain.User} entity.
 */
public class UserCriteria implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    private String login;

    private String firstName;

    private String lastName;

    private String email;

    private Boolean activated;

    private String langKey;

    public UserCriteria() {}

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getLogin() {
        return login;
    }

    public void setLogin(String login) {
        this.login = login;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Boolean getActivated() {
        return activated;
    }

    public void setActivated(Boolean activated) {
        this.activated = activated;
    }

    public String getLangKey() {
        return langKey;
    }

    public void setLangKey(String langKey) {
        this.langKey = langKey;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UserCriteria that = (UserCriteria) o;
        return Objects.equals(id, that.id) &&
            Objects.equals(login, that.login) &&
            Objects.equals(firstName, that.firstName) &&
            Objects.equals(lastName, that.lastName) &&
            Objects.equals(email, that.email) &&
            Objects.equals(activated, that.activated) &&
            Objects.equals(langKey, that.langKey);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, login, firstName, lastName, email, activated, langKey);
    }

    @Override
    public String toString() {
        return "UserCriteria{" +
            "id=" + id +
            ", login='" + login + '\'' +
            ", firstName='" + firstName + '\'' +
            ", lastName='" + lastName + '\'' +
            ", email='" + email + '\'' +
            ", activated=" + activated +
            ", langKey='" + langKey + '\'' +
            '}';
    }
}
