package com.repoalvo.javaapi;

import com.repoalvo.javaapi.model.UserCreateRequest;
import com.repoalvo.javaapi.model.UserResponse;
import com.repoalvo.javaapi.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class UserServiceUnitTest {

    private UserService userService;

    @BeforeEach
    void setup() {
        userService = new UserService();
    }

    @Test
    void getByIdShouldReturnUserWhenExists() {
        Optional<UserResponse> result = userService.getById(1);

        assertThat(result).isPresent();
        assertThat(result.get().id()).isEqualTo(1);
        assertThat(result.get().name()).isEqualTo("Ana Silva");
        assertThat(result.get().email()).isEqualTo("ana@example.com");
    }

    @Test
    void getByIdShouldReturnEmptyWhenUserDoesNotExist() {
        Optional<UserResponse> result = userService.getById(999);

        assertThat(result).isNotPresent();
    }

    @Test
    void listAllUsersShouldReturnPreloadedUsers() {
        List<UserResponse> users = userService.listAllUsers();

        assertThat(users).hasSize(2);
    }

    @Test
    void listUsersShouldRespectLimitAndOffset() {
        List<UserResponse> page = userService.listUsers(1, 0);

        assertThat(page).hasSize(1);
        assertThat(page.get(0).id()).isEqualTo(1);
    }

    @Test
    void listUsersShouldReturnEmptyWhenOffsetBeyondSize() {
        List<UserResponse> page = userService.listUsers(10, 100);

        assertThat(page).isEmpty();
    }

    @Test
    void findByEmailShouldReturnUserWhenEmailMatches() {
        Optional<UserResponse> result = userService.findByEmail("ana@example.com");

        assertThat(result).isPresent();
        assertThat(result.get().name()).isEqualTo("Ana Silva");
    }

    @Test
    void findByEmailShouldReturnEmptyWhenEmailNotFound() {
        Optional<UserResponse> result = userService.findByEmail("notfound@example.com");

        assertThat(result).isNotPresent();
    }

    @Test
    void createShouldAddUserAndReturnWithGeneratedId() {
        UserCreateRequest request = new UserCreateRequest("Carlos Souza", "carlos@example.com");

        UserResponse created = userService.create(request);

        assertThat(created.id()).isGreaterThan(0);
        assertThat(created.name()).isEqualTo("Carlos Souza");
        assertThat(created.email()).isEqualTo("carlos@example.com");
        assertThat(userService.getById(created.id())).isPresent();
    }
}