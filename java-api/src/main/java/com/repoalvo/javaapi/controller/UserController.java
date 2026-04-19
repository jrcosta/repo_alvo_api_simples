package com.repoalvo.javaapi.controller;

import com.repoalvo.javaapi.model.AgeEstimateResponse;
import com.repoalvo.javaapi.model.CountResponse;
import com.repoalvo.javaapi.model.EmailResponse;
import com.repoalvo.javaapi.model.HealthResponse;
import com.repoalvo.javaapi.model.UserCreateRequest;
import com.repoalvo.javaapi.model.UserExistsResponse;
import com.repoalvo.javaapi.model.UserResponse;
import com.repoalvo.javaapi.service.ExternalService;
import com.repoalvo.javaapi.service.UserService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping
@Validated
public class UserController {

    private final UserService userService;
    private final ExternalService externalService;

    public UserController(UserService userService, ExternalService externalService) {
        this.userService = userService;
        this.externalService = externalService;
    }

    @GetMapping("/health")
    public HealthResponse healthcheck() {
        return new HealthResponse("ok");
    }

    @GetMapping("/users")
    public List<UserResponse> listUsers(
            @RequestParam(defaultValue = "100") @Min(1) int limit,
            @RequestParam(defaultValue = "0") @Min(0) int offset
    ) {
        return userService.listUsers(limit, offset);
    }

    @GetMapping("/users/count")
    public CountResponse usersCount() {
        return new CountResponse(userService.listAllUsers().size());
    }

    @PostMapping("/users")
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse createUser(@Valid @RequestBody UserCreateRequest payload) {
        if (userService.findByEmail(payload.email()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "E-mail já cadastrado");
        }

        return userService.create(payload);
    }

    @GetMapping("/users/first-email")
    public UserResponse firstUserEmail() {
        List<UserResponse> users = userService.listAllUsers();

        if (users.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Nenhum usuário encontrado");
        }

        return users.getFirst();
    }

    @GetMapping("/users/broken")
    public Map<String, Integer> usersBroken() {
        return Map.of("total", userService.listAllUsers().size());
    }

    @GetMapping("/users/{userId}/email")
    public EmailResponse getUserEmail(@PathVariable int userId) {
        UserResponse user = userService.getById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuário não encontrado"));

        return new EmailResponse(user.email());
    }

    @GetMapping("/users/{userId}/exists")
    public UserExistsResponse userExists(@PathVariable int userId) {
        return new UserExistsResponse(userService.getById(userId).isPresent());
    }

    @GetMapping("/users/search")
    public List<UserResponse> searchUsers(@RequestParam String q) {
        String term = q.toLowerCase();
        return userService.listAllUsers()
                .stream()
                .filter(u -> u.name().toLowerCase().contains(term))
                .toList();
    }

    @GetMapping("/users/names")
    public List<String> listUserNames() {
        List<UserResponse> users = userService.listAllUsers();
        if (users == null) {
            return List.of();
        }
        return users.stream()
                .map(UserResponse::name)
                .sorted(String::compareToIgnoreCase)
                .toList();
    }

    @GetMapping("/users/duplicates")
    public List<UserResponse> findDuplicateUsers() {
        List<UserResponse> allUsers = userService.listAllUsers();
        Map<String, Long> emailCounts = allUsers.stream()
                .collect(Collectors.groupingBy(UserResponse::email, Collectors.counting()));

        return allUsers.stream()
                .filter(u -> emailCounts.getOrDefault(u.email(), 0L) > 1)
                .toList();
    }

    @GetMapping("/users/email-domains")
    public Map<String, Long> usersByEmailDomain() {
        return userService.listAllUsers()
                .stream()
                .map(UserResponse::email)
                .filter(email -> email.contains("@"))
                .map(email -> email.substring(email.indexOf('@') + 1).toLowerCase())
                .collect(Collectors.groupingBy(domain -> domain, Collectors.counting()));
    }

    @GetMapping("/users/{userId}/age-estimate")
    public AgeEstimateResponse getUserAgeEstimate(@PathVariable int userId) {
        UserResponse user = userService.getById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuário não encontrado"));

        return externalService.estimateAge(user.name());
    }

    @GetMapping("/users/{userId}")
    public UserResponse getUser(@PathVariable int userId) {
        return userService.getById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuário não encontrado"));
    }
}
