package com.nhom10.coolmate.vouchers;

import com.nhom10.coolmate.exception.AppException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class VoucherService {

    private final VoucherRepository voucherRepository;

    // --- Mapper ---
    private VoucherDTO mapToDTO(Voucher voucher) {
        return VoucherDTO.builder()
                .id(voucher.getId())
                .code(voucher.getCode())
                .description(voucher.getDescription())
                .discountType(voucher.getDiscountType())

                // MAP: Entity.discountAmount -> DTO.discountValue
                .discountValue(voucher.getDiscountAmount())

                .minOrderAmount(voucher.getMinOrder())
                .startDate(voucher.getStartDate())
                .endDate(voucher.getEndDate())
                .usageLimit(voucher.getUsageLimit())
                .usedCount(voucher.getUsedCount())
                .status(voucher.getStatus())
                .build();
    }

    private Voucher mapToEntity(VoucherDTO dto) {
        Voucher voucher = Voucher.builder()
                .id(dto.getId())
                .code(dto.getCode().toUpperCase())
                .description(dto.getDescription())
                .discountType(dto.getDiscountType())

                // MAP: DTO.discountValue -> Entity.discountAmount
                .discountAmount(dto.getDiscountValue())

                .minOrder(dto.getMinOrderAmount())
                .quantity(dto.getUsageLimit() != null ? dto.getUsageLimit() : 100) // Mặc định quantity
                .startDate(dto.getStartDate())
                .endDate(dto.getEndDate())
                .usageLimit(dto.getUsageLimit())
                .usedCount(dto.getUsedCount())
                .status(dto.getStatus())
                .build();

        if (voucher.getId() == null) {
            voucher.setUsedCount(0);
            if (voucher.getQuantity() == null) voucher.setQuantity(100);
            if (voucher.getStatus() == null) voucher.setStatus(1);
        }
        return voucher;
    }

    // --- CRUD Logic ---

    // 1. READ: Lấy tất cả Vouchers
    public List<VoucherDTO> getAllVouchers() {
        return voucherRepository.findAll().stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    // 2. READ: Lấy chi tiết Voucher theo ID
    public VoucherDTO getVoucherById(Integer id) {
        Voucher voucher = voucherRepository.findById(id)
                .orElseThrow(() -> new AppException("Voucher không tìm thấy với ID: " + id));
        return mapToDTO(voucher);
    }

    // 3. SAVE: Thêm mới hoặc Cập nhật Voucher
    @Transactional
    public VoucherDTO saveVoucher(VoucherDTO dto) {
        dto.setCode(dto.getCode().toUpperCase());

        // Kiểm tra trùng CODE (không phân biệt hoa thường khi kiểm tra)
        voucherRepository.findByCodeIgnoreCase(dto.getCode()).ifPresent(existing -> {
            if (dto.getId() == null || !existing.getId().equals(dto.getId())) {
                throw new AppException("Mã Voucher đã tồn tại: " + dto.getCode());
            }
        });

        // Kiểm tra logic ngày tháng (Dùng isBefore cho LocalDate)
        if (dto.getStartDate() != null && dto.getEndDate() != null && dto.getEndDate().isBefore(dto.getStartDate())) {
            throw new AppException("Ngày kết thúc không được nhỏ hơn ngày bắt đầu.");
        }

        Voucher voucherToSave;
        if (dto.getId() == null) {
            voucherToSave = mapToEntity(dto);
        } else {
            Voucher existing = voucherRepository.findById(dto.getId())
                    .orElseThrow(() -> new AppException("Voucher không tìm thấy để cập nhật: " + dto.getId()));

            voucherToSave = mapToEntity(dto);
            voucherToSave.setUsedCount(existing.getUsedCount());
        }

        Voucher savedVoucher = voucherRepository.save(voucherToSave);
        return mapToDTO(savedVoucher);
    }

    // 4. DELETE: Xóa Voucher
    @Transactional
    public void deleteVoucher(Integer id) {
        if (!voucherRepository.existsById(id)) {
            throw new AppException("Voucher không tìm thấy để xóa.");
        }
        voucherRepository.deleteById(id);
    }

    // 5. Lấy danh sách voucher (Cho User chọn)
    public List<Voucher> getAvailableVouchers() {
        // Hàm findAllActiveVouchers cần được khai báo trong Repository
        return voucherRepository.findAllActiveVouchers();
    }

    // 6. Logic kiểm tra mã Voucher (Cho Checkout)
    public Voucher validateVoucher(String code, BigDecimal orderTotal) {
        // SỬA: Dùng findByCodeIgnoreCase để không phân biệt hoa thường
        Voucher voucher = voucherRepository.findByCodeIgnoreCase(code)
                .orElseThrow(() -> new AppException("Mã giảm giá không tồn tại."));

        // Kiểm tra trạng thái và số lượng
        if (voucher.getStatus() == 0 || voucher.getQuantity() <= 0) {
            throw new AppException("Mã giảm giá đã hết lượt sử dụng hoặc ngừng hoạt động.");
        }

        // SỬA: Dùng LocalDate.now() và isBefore/isAfter
        LocalDate now = LocalDate.now();

        if (voucher.getStartDate() != null && voucher.getStartDate().isAfter(now)) {
            throw new AppException("Mã giảm giá chưa đến đợt sử dụng.");
        }

        if (voucher.getEndDate() != null && voucher.getEndDate().isBefore(now)) {
            throw new AppException("Mã giảm giá đã hết hạn.");
        }

        // Kiểm tra giá trị đơn hàng tối thiểu
        if (voucher.getMinOrder() != null && orderTotal.compareTo(voucher.getMinOrder()) < 0) {
            throw new AppException("Đơn hàng chưa đạt giá trị tối thiểu ("
                    + voucher.getMinOrder() + ") để áp dụng mã này.");
        }

        return voucher;
    }

    // 7. Hàm TÍNH TOÁN SỐ TIỀN GIẢM THỰC TẾ (MỚI)
    /**
     * Tính số tiền giảm giá thực tế dựa trên Voucher và tổng đơn hàng.
     * Hàm này được gọi trong CheckoutController (API check) và OrderService (createOrder).
     * @param voucher Voucher Entity
     * @param orderTotal Tổng tiền đơn hàng chưa giảm
     * @return Số tiền giảm giá BigDecimal
     */
    public BigDecimal calculateDiscount(Voucher voucher, BigDecimal orderTotal) {
        if (voucher.getDiscountType() == DiscountType.PERCENT) {
            // Tính số tiền giảm theo %: total * (discountAmount / 100)
            BigDecimal percent = voucher.getDiscountAmount().divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            return orderTotal.multiply(percent).setScale(0, RoundingMode.HALF_UP); // Làm tròn về số nguyên
        } else if (voucher.getDiscountType() == DiscountType.AMOUNT) {
            // Giảm theo số tiền cố định, không được giảm quá tổng tiền
            return voucher.getDiscountAmount().min(orderTotal).setScale(0, RoundingMode.HALF_UP);
        }
        return BigDecimal.ZERO;
    }

    // 8. Tăng số lần sử dụng Voucher
    @Transactional
    public void increaseVoucherUsedCount(Voucher voucher) {
        voucher.setUsedCount(voucher.getUsedCount() + 1);
        voucher.setQuantity(voucher.getQuantity() - 1); // Giảm số lượng còn lại
        voucherRepository.save(voucher);
    }
}