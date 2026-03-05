package tz.co.iseke.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import tz.co.iseke.dto.AuthPayloadDTO;
import tz.co.iseke.inputs.CreateUserInput;
import tz.co.iseke.entity.User;
import tz.co.iseke.service.AuthService;

@Controller
@RequiredArgsConstructor
public class AuthResolver {

    private final AuthService authService;

    @MutationMapping
    public AuthPayloadDTO login(@Argument String username, @Argument String password) {
        return authService.login(username, password);
    }

    @MutationMapping
    public Boolean changePassword(@Argument String oldPassword, @Argument String newPassword) {
        return authService.changePassword(oldPassword, newPassword);
    }
    @MutationMapping
    @PreAuthorize("hasRole('ADMIN')")
    public User createUser(@Argument CreateUserInput input) {
        return authService.createUser(input);
    }
}