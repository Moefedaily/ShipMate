package com.shipmate.repository.user;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.shipmate.model.user.User;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID>,
                JpaSpecificationExecutor<User>  {
    Optional<User> findByEmail(String email);
    

}
