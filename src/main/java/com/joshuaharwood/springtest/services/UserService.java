package com.joshuaharwood.springtest.services;

import com.joshuaharwood.springtest.entities.User;
import com.joshuaharwood.springtest.repositories.UserRepository;
import java.util.Objects;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Services define business methods/logic and decouple our consumers' dependence on Spring Data.
 * Even if we're just calling straight through to the repository, this service acts as the TX
 * boundary.
 * <p>
 * Note: REQUIRED TxAttr should be fine for this, default for Spring also
 */
@Transactional(readOnly = true) // We can skip a Hibernate dirty-check/flush here with this!
@Service
public class UserService {

  private final UserRepository userRepository;

  public UserService(UserRepository userRepository) {
    this.userRepository = userRepository;
  }

  @Transactional
  public User saveUser(User user) {
    return userRepository.save(user);
  }

  public Optional<User> findById(long id) {
    return userRepository.findById(id);
  }

  @Transactional
  public void deleteById(long id) {
    userRepository.deleteById(id);
  }

  public Page<User> findAllUsersByFirstName(String firstName, Pageable pageable) {
    return userRepository.findByFirstName(firstName, pageable);
  }

  public Page<User> findAllUsersByLastName(String lastName, Pageable pageable) {
    return userRepository.findByLastName(lastName, pageable);
  }

  public Page<User> findAllUsers(Pageable pageable) {
    return userRepository.findAll(pageable);
  }

  // This could also be done as `boolean existsByIdAndFirstName(long id, String firstName);` in the repo!
  public boolean userIsCool(long id) {
    return userRepository.findById(id)
        .filter(user -> Objects.equals(user.getFirstName(), "Josh"))
        .isPresent();
  }
}
