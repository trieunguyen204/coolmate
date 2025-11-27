package com.nhom10.coolmate.vouchers;

import com.nhom10.coolmate.exception.ResourceNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@Transactional
public class VoucherService {

    @Autowired
    private VoucherRepository voucherRepository;

    // 1. Lấy danh sách
    public List<Voucher> getAllVouchers() {
        return voucherRepository.findAll();
    }

    // 2. Lấy chi tiết
    public Voucher getVoucherById(Integer id) {
        return voucherRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Voucher", "ID", id));
    }

    // 3. Lưu (Thêm mới & Cập nhật) - Có Validate
    public void saveVoucher(Voucher voucher) {
        // A. Validate Mã trùng (Chỉ check khi thêm mới)
        if (voucher.getId() == null && voucherRepository.existsByCode(voucher.getCode())) {
            throw new IllegalArgumentException("Mã Voucher '" + voucher.getCode() + "' đã tồn tại!");
        }

        // B. Validate Ngày tháng
        if (voucher.getStartDate() != null && voucher.getEndDate() != null) {
            // Ngày kết thúc phải sau hoặc bằng ngày bắt đầu
            if (voucher.getEndDate().isBefore(voucher.getStartDate())) {
                throw new IllegalArgumentException("Lỗi: Ngày kết thúc phải lớn hơn hoặc bằng ngày bắt đầu!");
            }
        }

        // C. Xử lý dữ liệu
        voucher.setCode(voucher.getCode().toUpperCase()); // Luôn viết hoa
        if (voucher.getStatus() == null) {
            voucher.setStatus(1); // Mặc định Active
        }

        // Nếu update, giữ nguyên số lượt đã dùng (nếu form không gửi lên)
        if (voucher.getId() != null && voucher.getUsedCount() == null) {
            Voucher oldVoucher = getVoucherById(voucher.getId());
            voucher.setUsedCount(oldVoucher.getUsedCount());
        }

        voucherRepository.save(voucher);
    }

    // 4. Xóa
    public void deleteVoucher(Integer id) {
        if (!voucherRepository.existsById(id)) {
            throw new ResourceNotFoundException("Voucher", "ID", id);
        }
        voucherRepository.deleteById(id);
    }
}