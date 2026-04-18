package com.repoalvo.javaapi;

import com.repoalvo.javaapi.model.User;
import com.repoalvo.javaapi.repository.UserRepository;
import com.repoalvo.javaapi.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

class UserServiceUnitTest {

    @Mock
    private UserRepository userRepository;

    private UserService userService;

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);
        userService = new UserService(userRepository);
    }

    @Test
    void getByIdShouldReturnUserWhenExists() {
        int userId = 1;
        User user = new User();
        user.setId(userId);
        user.setName("Test User");
        user.setEmail("test@example.com");

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        Optional<User> result = userService.getById(userId);

        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(userId);
        assertThat(result.get().getName()).isEqualTo("Test User");
        assertThat(result.get().getEmail()).isEqualTo("test@example.com");
    }

    @Test
    void getByIdShouldReturnEmptyWhenUserDoesNotExist() {
        int userId = 999;

        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        Optional<User> result = userService.getById(userId);

        assertThat(result).isNotPresent();
    }
}