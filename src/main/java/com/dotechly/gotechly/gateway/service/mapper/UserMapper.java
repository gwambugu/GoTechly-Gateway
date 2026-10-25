package com.dotechly.gotechly.gateway.service.mapper;

import com.dotechly.gotechly.gateway.domain.Authority;
import com.dotechly.gotechly.gateway.domain.User;
import com.dotechly.gotechly.gateway.service.dto.AdminUserDTO;
import com.dotechly.gotechly.gateway.service.dto.UserDTO;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Mapper for the entity {@link User} and its DTO {@link UserDTO}.
 */
@Service
public class UserMapper {

    public AdminUserDTO userToAdminUserDTO(User user) {
        if (user == null) {
            return null;
        }
        return new AdminUserDTO(user);
    }

    public User adminUserDTOToUser(AdminUserDTO adminUserDTO) {
        if (adminUserDTO == null) {
            return null;
        }
        User user = new User();
        user.setId(adminUserDTO.getId());
        user.setLogin(adminUserDTO.getLogin());
        user.setFirstName(adminUserDTO.getFirstName());
        user.setLastName(adminUserDTO.getLastName());
        user.setEmail(adminUserDTO.getEmail());
        user.setActivated(adminUserDTO.isActivated());
        user.setImageUrl(adminUserDTO.getImageUrl());
        user.setLangKey(adminUserDTO.getLangKey());
        user.setAuthorities(stringSetToAuthorities(adminUserDTO.getAuthorities()));
        return user;
    }

    public List<UserDTO> usersToUserDTOs(List<User> users) {
        if (users == null) {
            return null;
        }
        return users.stream().filter(Objects::nonNull).map(this::userToUserDTO).collect(Collectors.toList());
    }

    public UserDTO userToUserDTO(User user) {
        if (user == null) {
            return null;
        }
        return new UserDTO(user);
    }

    public User userDTOToUser(UserDTO userDTO) {
        if (userDTO == null) {
            return null;
        }
        User user = new User();
        user.setId(userDTO.getId());
        user.setLogin(userDTO.getLogin());
        user.setFirstName(userDTO.getFirstName());
        user.setLastName(userDTO.getLastName());
        user.setEmail(userDTO.getEmail());
        user.setActivated(userDTO.isActivated());
        user.setImageUrl(userDTO.getImageUrl());
        user.setLangKey(userDTO.getLangKey());
        return user;
    }

    public Set<String> authoritiesToStringSet(Set<Authority> authorities) {
        if (authorities == null) {
            return Collections.emptySet();
        }
        return authorities.stream().map(Authority::getName).collect(Collectors.toSet());
    }

    public Set<Authority> stringSetToAuthorities(Set<String> authoritiesAsString) {
        if (authoritiesAsString == null) {
            return Collections.emptySet();
        }
        return authoritiesAsString.stream().map(name -> {
            Authority auth = new Authority();
            auth.setName(name);
            return auth;
        }).collect(Collectors.toSet());
    }

    public User userFromId(Long id) {
        if (id == null) {
            return null;
        }
        User user = new User();
        user.setId(id);
        return user;
    }
}
