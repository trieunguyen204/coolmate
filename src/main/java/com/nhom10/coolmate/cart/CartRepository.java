package com.nhom10.coolmate.cart;

import com.nhom10.coolmate.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CartRepository extends JpaRepository<Cart, Integer> {
    Optional<Cart> findByUser(User user);

    // THÊM: Tìm theo session token
    Optional<Cart> findBySessionToken(String sessionToken);
}