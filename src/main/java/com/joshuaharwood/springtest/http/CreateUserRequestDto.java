package com.joshuaharwood.springtest.http;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Past;
import java.time.LocalDate;

/**
 * DTO for {@link com.joshuaharwood.springtest.entities.User}
 */
public record CreateUserRequestDto(@NotBlank String firstName,
                                   @NotBlank String lastName,
                                   @NotBlank @Past LocalDate dateOfBirth) {

}