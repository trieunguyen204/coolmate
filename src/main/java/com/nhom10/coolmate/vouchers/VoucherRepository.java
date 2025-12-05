package com.nhom10.coolmate.vouchers;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;

import java.util.Optional;

@Repository
public interface  VoucherRepository extends JpaRepository<Voucher, Integer> {
    // Tìm kiếm voucher theo code (không phân biệt hoa thường)
    Optional<Voucher> findByCodeIgnoreCase(String code);

    // Kiểm tra tồn tại theo code
    boolean existsByCodeIgnoreCase(String code);

    Optional<Voucher> findByCode(String code);

    @Query("SELECT v FROM Voucher v WHERE v.status = 1 AND v.quantity > 0 AND (v.endDate >= CURRENT_DATE) ORDER BY v.discountAmount DESC")
    List<Voucher> findAllActiveVouchers();
}