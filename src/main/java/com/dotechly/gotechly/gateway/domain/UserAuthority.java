package com.dotechly.gotechly.gateway.domain;

import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.io.Serializable;
import java.util.Objects;

/**
 * A user_authority join table entity.
 */

@Table("user_authority")
public class UserAuthority implements Serializable {

    private static final long serialVersionUID = 1L;

    @Column("user_id")
    private Long userId;

    @Column("authority_name")
    private String authorityName;

    public UserAuthority() {}

    public UserAuthority(Long userId, String authorityName) {
        this.userId = userId;
        this.authorityName = authorityName;
    }


    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getAuthorityName() {
        return authorityName;
    }

    public void setAuthorityName(String authorityName) {
        this.authorityName = authorityName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UserAuthority that = (UserAuthority) o;
        return Objects.equals(userId, that.userId) &&
            Objects.equals(authorityName, that.authorityName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId, authorityName);
    }

    @Override
    public String toString() {
        return "UserAuthority{" +
            "userId=" + userId +
            ", authorityName='" + authorityName + '\'' +
            '}';
    }
}
