package com.nhom10.coolmate.vouchers;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface VoucherRepository extends JpaRepository<Voucher, Integer> {
    // Tìm kiếm voucher theo code (không phân biệt hoa thường)
    Optional<Voucher> findByCodeIgnoreCase(String code);

    // Kiểm tra tồn tại theo code
    boolean existsByCodeIgnoreCase(String code);
}