package com.nhom10.coolmate.address;

import com.nhom10.coolmate.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AddressRepository extends JpaRepository<Address, Integer> {
    // Tìm tất cả địa chỉ của 1 user
    List<Address> findByUser(User user);

    // Tìm địa chỉ mặc định của user (isDefault = 1)
    Optional<Address> findByUserAndIsDefault(User user, Integer isDefault);
}