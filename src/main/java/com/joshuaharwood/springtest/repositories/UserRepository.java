package com.joshuaharwood.springtest.repositories;

import com.joshuaharwood.springtest.entities.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {

  Page<User> findByFirstName(String name, Pageable pageable);

  Page<User> findByLastName(String name, Pageable pageable);
}