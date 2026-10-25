package com.dotechly.gotechly.gateway.web.rest;

import com.dotechly.gotechly.gateway.service.UserService;
import com.dotechly.gotechly.gateway.service.dto.AdminUserDTO;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Set;

/**
 * REST controller for managing users.
 */
@RestController
@RequestMapping("/api/admin/users")
public class UserResource {

    private final UserService userService;

    public UserResource(UserService userService) {
        this.userService = userService;
    }

    /**
     * {@code POST  /users}  : Creates a new user.
     *
     * @param adminUserDTO the user to create.
     * @return the {@link ResponseEntity} with status {@code 201 (Created)} and with body the new user.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PostMapping
    public Mono<ResponseEntity<AdminUserDTO>> createUser(@Valid @RequestBody AdminUserDTO adminUserDTO) throws URISyntaxException {
        if (adminUserDTO.getId() != null) {
            return Mono.just(ResponseEntity.badRequest().build());
        }
        return userService.createUser(adminUserDTO)
            .map(AdminUserDTO::new)
            .map(result -> {
                try {
                    return ResponseEntity.created(new URI("/api/admin/users/" + result.getLogin())).body(result);
                } catch (URISyntaxException e) {
                    throw new RuntimeException(e);
                }
            });
    }

    /**
     * {@code PUT  /users} : Updates an existing User.
     *
     * @param adminUserDTO the user to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated user.
     */
    @PutMapping
    public Mono<ResponseEntity<AdminUserDTO>> updateUser(@Valid @RequestBody AdminUserDTO adminUserDTO) {
        if (adminUserDTO.getId() == null) {
            return Mono.just(ResponseEntity.badRequest().build());
        }
        return userService.updateUser(adminUserDTO)
            .map(ResponseEntity::ok)
            .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    /**
     * {@code GET  /users} : get all users with pagination.
     *
     * @param page the page number (0-based).
     * @param size the page size.
     * @param response the server HTTP response (for setting total count header).
     * @return the {@link Flux} with status {@code 200 (OK)} and with body all users.
     */
    @GetMapping
    public Flux<AdminUserDTO> getAllUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            ServerHttpResponse response) {
        return userService.countUsers()
            .doOnNext(total -> response.getHeaders().add("X-Total-Count", String.valueOf(total)))
            .thenMany(userService.getAllManagedUsers(PageRequest.of(page, size)));
    }

    /**
     * {@code GET  /users/:login} : get the "login" user.
     *
     * @param login the login of the user to find.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the "login" user, or with status {@code 404 (Not Found)}.
     */
    @GetMapping("/{login}")
    public Mono<ResponseEntity<AdminUserDTO>> getUser(@PathVariable String login) {
        return userService.getUserWithAuthoritiesByLogin(login)
            .map(ResponseEntity::ok)
            .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    /**
     * {@code DELETE  /users/:id} : delete the User by id.
     *
     * @param id the id of the user to delete.
     * @return the {@link ResponseEntity} with status {@code 204 (NO_CONTENT)}.
     */
    @DeleteMapping("/{id}")
    public Mono<ResponseEntity<Void>> deleteUser(@PathVariable Long id) {
        return userService.deleteUserById(id).thenReturn(ResponseEntity.noContent().build());
    }

    /**
     * {@code GET  /users/pending} : get all pending users.
     *
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body all pending users.
     */
    @GetMapping("/pending")
    public Flux<AdminUserDTO> getPendingUsers() {
        return userService.getPendingUsers();
    }

    /**
     * {@code POST  /users/:id/approve} : approve a user.
     *
     * @param id the id of the user to approve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)}.
     */
    @PostMapping("/{id}/approve")
    public Mono<ResponseEntity<Void>> approveUser(@PathVariable Long id) {
        return userService.approveUser(id).thenReturn(ResponseEntity.ok().build());
    }

    /**
     * {@code GET  /users/search} : search users by login or email.
     *
     * @param query the search query.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body matching users.
     */
    @GetMapping("/search")
    public Flux<AdminUserDTO> searchUsers(@RequestParam String q) {
        return userService.searchUsers(q);
    }

    /**
     * {@code PUT  /users/:id/roles} : update user roles.
     *
     * @param id the id of the user.
     * @param authorities the new authorities.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)}.
     */
    @PutMapping("/{id}/roles")
    public Mono<ResponseEntity<Void>> updateUserRoles(@PathVariable Long id, @RequestBody Set<String> authorities) {
        return userService.updateUserRoles(id, authorities).thenReturn(ResponseEntity.ok().build());
    }

    /**
     * {@code PUT  /users/:id/enabled} : enable or disable a user.
     *
     * @param id the id of the user.
     * @param enabled the new enabled state.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)}.
     */
    @PutMapping("/{id}/enabled")
    public Mono<ResponseEntity<Void>> setUserEnabled(@PathVariable Long id, @RequestBody boolean enabled) {
        return userService.setUserEnabled(id, enabled).thenReturn(ResponseEntity.ok().build());
    }

    /**
     * {@code GET  /users/online} : get all online (activated) users.
     *
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body all online users.
     */
    @GetMapping("/online")
    public Flux<AdminUserDTO> getOnlineUsers() {
        return userService.getOnlineUsers();
    }
}
