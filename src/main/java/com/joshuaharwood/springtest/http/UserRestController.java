package com.joshuaharwood.springtest.http;

import com.joshuaharwood.springtest.entities.User;
import com.joshuaharwood.springtest.services.UserService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import java.net.URI;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

/**
 * The controller here calls down to the UserService, that way we decouple the HTTP layer from
 * Spring Data as an implementation detail. Threw in some Bean Validation for fun.
 * TODO: Error handling isn't the best here: extension work?
 */
@RestController
@RequestMapping("/users")
class UserRestController {

  private final UserService userService;

  // Note: Autowired is implicit when 1 constructor
  public UserRestController(UserService userService) {
    this.userService = userService;
  }

  // -- GETS --

  @GetMapping("/{id}")
  public ResponseEntity<UserDto> getUserById(@PathVariable @Positive long id) {
    return ResponseEntity.of(userService.findById(id).map(UserRestController::convertUser));
  }

  @GetMapping("/{id}/cool")
  public boolean isUserCool(@PathVariable @Positive long id) {
    return userService.userIsCool(id);
  }

  @GetMapping
  public Page<UserDto> getAllUsers(Pageable pageable) {
    return userService.findAllUsers(pageable).map(UserRestController::convertUser);
  }

  @GetMapping(params = "firstName")
  public Page<UserDto> getAllUsersWithFirstName(@NotBlank String firstName, Pageable pageable) {
    return userService.findAllUsersByFirstName(firstName, pageable)
        .map(UserRestController::convertUser);
  }

  @GetMapping(params = "lastName")
  public Page<UserDto> getAllUsersWithLastName(@NotBlank String lastName, Pageable pageable) {
    return userService.findAllUsersByLastName(lastName, pageable)
        .map(UserRestController::convertUser);
  }

  // -- POSTs --

  /**
   * POST - not idempotent, running it twice would create 2 users (no ID in the request supplied).
   * If an ID was supplied, it would override and be idempotent, therefore a PUT.
   */
  @PostMapping
  public ResponseEntity<UserDto> createUser(@Valid @RequestBody CreateUserRequestDto req) {
    final User newUser = userService.saveUser(
        new User(req.firstName(), req.lastName(), req.dateOfBirth()));

    final URI uri = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}")
        .buildAndExpand(newUser.getId()).toUri();

    return ResponseEntity.created(uri).body(
        new UserDto(newUser.getId(), newUser.getFirstName(), newUser.getLastName(),
            newUser.getDateOfBirth()));
  }

  /**
   * delete user - idempotent -> server is in the same -state- after repeating it. Always returns
   * 204! DELETE doesn't have to be idempotent, the other convention is 404 if missing, 204 if
   * deletion successful. This is the idempotent version!
   */
  @DeleteMapping
  public ResponseEntity<Void> deleteUser(@Positive long id) {
    userService.deleteById(id);

    return ResponseEntity.noContent().build();
  }

  private static UserDto convertUser(User user) {
    return new UserDto(user.getId(), user.getFirstName(), user.getLastName(),
        user.getDateOfBirth());
  }
}
