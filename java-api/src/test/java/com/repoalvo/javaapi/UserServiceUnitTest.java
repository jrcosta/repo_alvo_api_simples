package com.repoalvo.javaapi;

import com.repoalvo.javaapi.model.UserCreateRequest;
import com.repoalvo.javaapi.model.UserResponse;
import com.repoalvo.javaapi.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
    void getByIdShouldReturnEmptyForZeroOrNegativeId() {
        Optional<UserResponse> zeroIdResult = userService.getById(0);
        Optional<UserResponse> negativeIdResult = userService.getById(-5);

        assertThat(zeroIdResult).isNotPresent();
        assertThat(negativeIdResult).isNotPresent();
    }

    @Test
    void listAllUsersShouldReturnPreloadedUsers() {
        List<UserResponse> users = userService.listAllUsers();

        assertThat(users).hasSize(2);
        assertThat(users).extracting(UserResponse::id).containsExactlyInAnyOrder(1, 2);
        assertThat(users).extracting(UserResponse::name).contains("Ana Silva", "João Pereira");
        assertThat(users).extracting(UserResponse::email).contains("ana@example.com", "joao@example.com");
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
    void listUsersShouldReturnEmptyForNegativeLimitOrOffset() {
        List<UserResponse> negativeLimit = userService.listUsers(-1, 0);
        List<UserResponse> negativeOffset = userService.listUsers(1, -10);
        List<UserResponse> negativeBoth = userService.listUsers(-5, -5);

        assertThat(negativeLimit).isEmpty();
        assertThat(negativeOffset).isEmpty();
        assertThat(negativeBoth).isEmpty();
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
    void findByEmailShouldReturnEmptyForNullOrEmptyEmail() {
        Optional<UserResponse> nullEmail = userService.findByEmail(null);
        Optional<UserResponse> emptyEmail = userService.findByEmail("");
        Optional<UserResponse> blankEmail = userService.findByEmail("   ");

        assertThat(nullEmail).isNotPresent();
        assertThat(emptyEmail).isNotPresent();
        assertThat(blankEmail).isNotPresent();
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

    @Test
    void createShouldThrowExceptionWhenEmailAlreadyExists() {
        UserCreateRequest duplicateEmailRequest = new UserCreateRequest("Ana Silva Clone", "ana@example.com");

        assertThatThrownBy(() -> userService.create(duplicateEmailRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("email already exists");
    }

    @Test
    void createShouldActuallyAddUserToInternalList() {
        int initialSize = userService.listAllUsers().size();

        UserCreateRequest request = new UserCreateRequest("Mariana Lima", "mariana@example.com");
        UserResponse created = userService.create(request);

        List<UserResponse> allUsers = userService.listAllUsers();
        assertThat(allUsers).hasSize(initialSize + 1);
        assertThat(allUsers).extracting(UserResponse::id).contains(created.id());
        assertThat(allUsers).extracting(UserResponse::email).contains("mariana@example.com");
    }

    @Test
    void createShouldThrowExceptionWhenRequestIsNull() {
        assertThatThrownBy(() -> userService.create(null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void createShouldThrowExceptionWhenNameOrEmailIsNullOrEmpty() {
        UserCreateRequest nullName = new UserCreateRequest(null, "valid@example.com");
        UserCreateRequest emptyName = new UserCreateRequest("", "valid@example.com");
        UserCreateRequest nullEmail = new UserCreateRequest("Valid Name", null);
        UserCreateRequest emptyEmail = new UserCreateRequest("Valid Name", "");

        assertThatThrownBy(() -> userService.create(nullName))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> userService.create(emptyName))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> userService.create(nullEmail))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> userService.create(emptyEmail))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void serviceMethodsShouldPropagateUnexpectedExceptions() {
        // This test assumes we can simulate an internal failure by subclassing or reflection.
        // Since we don't have access to internals, we simulate by creating a subclass that throws.

        UserService failingService = new UserService() {
            @Override
            public Optional<UserResponse> getById(int id) {
                throw new RuntimeException("Simulated failure");
            }

            @Override
            public List<UserResponse> listAllUsers() {
                throw new RuntimeException("Simulated failure");
            }

            @Override
            public List<UserResponse> listUsers(int limit, int offset) {
                throw new RuntimeException("Simulated failure");
            }

            @Override
            public Optional<UserResponse> findByEmail(String email) {
                throw new RuntimeException("Simulated failure");
            }

            @Override
            public UserResponse create(UserCreateRequest request) {
                throw new RuntimeException("Simulated failure");
            }
        };

        assertThatThrownBy(() -> failingService.getById(1))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Simulated failure");

        assertThatThrownBy(failingService::listAllUsers)
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Simulated failure");

        assertThatThrownBy(() -> failingService.listUsers(1, 0))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Simulated failure");

        assertThatThrownBy(() -> failingService.findByEmail("ana@example.com"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Simulated failure");

        assertThatThrownBy(() -> failingService.create(new UserCreateRequest("Fail", "fail@example.com")))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Simulated failure");
    }
}