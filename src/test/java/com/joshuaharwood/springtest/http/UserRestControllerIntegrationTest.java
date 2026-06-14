package com.joshuaharwood.springtest.http;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.joshuaharwood.springtest.TestcontainersConfiguration;
import com.joshuaharwood.springtest.entities.User;
import com.joshuaharwood.springtest.repositories.UserRepository;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Full-stack integration test: the real Spring context (controller -> service -> repository -> JPA)
 * runs against an actual MySQL database started in Docker by {@link TestcontainersConfiguration}.
 *
 * <p>Requests go through {@link MockMvc} (in-process transport), but every database call is real:
 * we assert against the injected {@link UserRepository} to prove rows are genuinely persisted to,
 * and removed from, the containerised MySQL. The table is cleared before each test for isolation.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class UserRestControllerIntegrationTest {

  private static final LocalDate DOB = LocalDate.of(1995, 5, 23);

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private UserRepository userRepository;

  @BeforeEach
  void cleanDatabase() {
    userRepository.deleteAll();
  }

  @Test
  void createUser_persistsRowToMySqlAndReturns201() throws Exception {
    final String body = """
        {"firstName":"Josh","lastName":"Harwood","dateOfBirth":"1995-05-23"}""";

    mockMvc.perform(post("/users")
            .contentType(MediaType.APPLICATION_JSON)
            .content(body))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").isNumber())
        .andExpect(jsonPath("$.firstName").value("Josh"));

    final List<User> all = userRepository.findAll();
    assertThat(all).singleElement().satisfies(u -> {
      assertThat(u.getFirstName()).isEqualTo("Josh");
      assertThat(u.getLastName()).isEqualTo("Harwood");
      assertThat(u.getDateOfBirth()).isEqualTo(DOB);
      // Proves JPA auditing (@CreatedDate / @LastModifiedDate) ran against the real DB.
      assertThat(u.getCreatedDate()).isNotNull();
      assertThat(u.getLastModifiedDate()).isNotNull();
    });
  }

  @Test
  void getUserById_returnsPersistedUser() throws Exception {
    final User saved = userRepository.save(new User("Josh", "Harwood", DOB));

    mockMvc.perform(get("/users/{id}", saved.getId()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(saved.getId()))
        .andExpect(jsonPath("$.firstName").value("Josh"))
        .andExpect(jsonPath("$.lastName").value("Harwood"))
        .andExpect(jsonPath("$.dateOfBirth").value("1995-05-23"));
  }

  @Test
  void getUserById_whenAbsentFromDb_returns404() throws Exception {
    mockMvc.perform(get("/users/{id}", 999_999))
        .andExpect(status().isNotFound());
  }

  @Test
  void deleteUser_removesRowFromMySql() throws Exception {
    final User saved = userRepository.save(new User("Josh", "Harwood", DOB));

    mockMvc.perform(delete("/users").param("id", String.valueOf(saved.getId())))
        .andExpect(status().isNoContent());

    assertThat(userRepository.existsById(saved.getId())).isFalse();
  }

  @Test
  void isUserCool_isTrueForJoshAndFalseForOthers() throws Exception {
    final User josh = userRepository.save(new User("Josh", "Harwood", DOB));
    final User dave = userRepository.save(new User("Dave", "Smith", DOB));

    mockMvc.perform(get("/users/{id}/cool", josh.getId()))
        .andExpect(status().isOk())
        .andExpect(content().string("true"));

    mockMvc.perform(get("/users/{id}/cool", dave.getId()))
        .andExpect(status().isOk())
        .andExpect(content().string("false"));
  }

  @Test
  void getAllUsers_returnsPersistedUsersAsPage() throws Exception {
    userRepository.save(new User("Josh", "Harwood", DOB));
    userRepository.save(new User("Dave", "Smith", DOB));

    mockMvc.perform(get("/users"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content.length()").value(2))
        .andExpect(jsonPath("$.page.totalElements").value(2));
  }

  @Test
  void getAllUsersWithFirstName_filtersInTheDatabase() throws Exception {
    userRepository.save(new User("Josh", "Harwood", DOB));
    userRepository.save(new User("Dave", "Smith", DOB));

    mockMvc.perform(get("/users").param("firstName", "Josh"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content.length()").value(1))
        .andExpect(jsonPath("$.content[0].firstName").value("Josh"));
  }
}
