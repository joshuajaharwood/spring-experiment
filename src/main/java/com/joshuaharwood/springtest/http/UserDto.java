package com.joshuaharwood.springtest.http;

import java.time.LocalDate;

/**
 * DTO for {@link com.joshuaharwood.springtest.entities.User}
 */
public record UserDto(long id, String firstName, String lastName, LocalDate dateOfBirth) {

}