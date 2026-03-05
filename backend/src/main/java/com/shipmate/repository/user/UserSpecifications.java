package com.shipmate.repository.user;

import com.shipmate.model.user.Role;
import com.shipmate.model.user.User;
import com.shipmate.model.user.UserType;

import org.springframework.data.jpa.domain.Specification;

import java.util.Locale;

public final class UserSpecifications {

    private UserSpecifications() {}

    public static Specification<User> hasRole(Role role) {

        if (role == null) {
            return null;
        }

        return (root, query, cb) ->
                cb.equal(root.get("role"), role);
    }

    public static Specification<User> hasUserType(UserType userType) {

        if (userType == null) {
            return null;
        }

        return (root, query, cb) ->
                cb.equal(root.get("userType"), userType);
    }

    public static Specification<User> isActive(Boolean active) {

        if (active == null) {
            return null;
        }

        return (root, query, cb) ->
                cb.equal(root.get("active"), active);
    }

    public static Specification<User> isVerified(Boolean verified) {
        if (verified == null) {
            return null;
        }

        return (root, query, cb) ->
                cb.equal(root.get("verified"), verified);
    }
    public static Specification<User> search(String term) {

        if (term == null || term.isBlank()) {
            return null;
        }

        String like = "%" + term.trim().toLowerCase(Locale.ROOT) + "%";

        return (root, query, cb) -> {

            if (query != null) {
                query.distinct(true);
            }

            return cb.or(
                    cb.like(cb.lower(root.get("email")), like),
                    cb.like(cb.lower(root.get("firstName")), like),
                    cb.like(cb.lower(root.get("lastName")), like)
            );
        };
    }
}