package tz.co.iseke.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import tz.co.iseke.inputs.UpdateUserInput;
import tz.co.iseke.entity.User;
import tz.co.iseke.service.UserService;

import java.util.List;
import java.util.UUID;

@Controller
@RequiredArgsConstructor
public class UserResolver {

    private final UserService userService;

    /**
     * Query: Find all users
     * Requires ADMIN or MANAGER role
     */
    @QueryMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public List<User> findAllUsers() {
        return userService.findAllUsers();
    }

    /**
     * Query: Find user by ID
     * Requires ADMIN or MANAGER role
     */
    @QueryMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public User findUserById(@Argument String id) {
        return userService.findUserById(UUID.fromString(id));
    }

    /**
     * Mutation: Update user
     * Requires ADMIN or MANAGER role
     */
    @MutationMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public User updateUser(@Argument String id, @Argument UpdateUserInput input) {
        return userService.updateUser(UUID.fromString(id), input);
    }

    /**
     * Mutation: Deactivate user (soft delete)
     * Requires ADMIN role only
     */
    @MutationMapping
    @PreAuthorize("hasRole('ADMIN')")
    public Boolean deactivateUser(@Argument String id) {
        return userService.deactivateUser(UUID.fromString(id));
    }

    /**
     * Mutation: Delete user (hard delete)
     * Requires ADMIN role only
     */
    @MutationMapping
    @PreAuthorize("hasRole('ADMIN')")
    public Boolean deleteUser(@Argument String id) {
        return userService.deleteUser(UUID.fromString(id));
    }

    /**
     * Mutation: Activate user
     * Requires ADMIN role only
     */
    @MutationMapping
    @PreAuthorize("hasRole('ADMIN')")
    public Boolean activateUser(@Argument String id) {
        return userService.activateUser(UUID.fromString(id));
    }

    /**
     * Mutation: Reset user password
     * Requires ADMIN role only
     */
    @MutationMapping
    @PreAuthorize("hasRole('ADMIN')")
    public Boolean resetPassword(@Argument String id, @Argument String newPassword) {
        return userService.resetPassword(UUID.fromString(id), newPassword);
    }
}
