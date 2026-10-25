package com.dotechly.gotechly.gateway.service.mapper;

import com.dotechly.gotechly.gateway.domain.Authority;
import com.dotechly.gotechly.gateway.service.dto.AuthorityDTO;
import org.springframework.stereotype.Service;

/**
 * Mapper for the entity {@link Authority} and its DTO {@link AuthorityDTO}.
 */
@Service
public class AuthorityMapper {

    public AuthorityDTO toDto(Authority authority) {
        if (authority == null) {
            return null;
        }
        AuthorityDTO authorityDTO = new AuthorityDTO();
        authorityDTO.setName(authority.getName());
        return authorityDTO;
    }

    public Authority toEntity(AuthorityDTO authorityDTO) {
        if (authorityDTO == null) {
            return null;
        }
        Authority authority = new Authority();
        authority.setName(authorityDTO.getName());
        return authority;
    }

    public Authority fromId(String name) {
        if (name == null) {
            return null;
        }
        Authority authority = new Authority();
        authority.setName(name);
        return authority;
    }
}
