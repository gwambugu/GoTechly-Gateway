package com.dotechly.gotechly.gateway.service.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.io.Serializable;
import java.util.Objects;

/**
 * A DTO representing an authority.
 */

public class AuthorityDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotNull
    @Size(max = 250)
    private String name;

    public AuthorityDTO() {}

    public AuthorityDTO(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AuthorityDTO that = (AuthorityDTO) o;
        return Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }

    @Override
    public String toString() {
        return "AuthorityDTO{" +
            "name='" + name + '\'' +
            '}';
    }
}
