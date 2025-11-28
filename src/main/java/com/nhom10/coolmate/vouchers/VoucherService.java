package com.nhom10.coolmate.vouchers;

import com.nhom10.coolmate.exception.AppException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZoneId;
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
                .discountValue(voucher.getDiscountValue())
                .minOrderAmount(voucher.getMinOrder())
                // Chuyển đổi java.sql.Date sang LocalDate
                .startDate(voucher.getStartDate() != null ? voucher.getStartDate().toLocalDate() : null)
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
                .discountValue(dto.getDiscountValue())
                .minOrder(dto.getMinOrderAmount())
                // Chuyển đổi LocalDate sang java.sql.Date
                .startDate(dto.getStartDate() != null ? java.sql.Date.valueOf(dto.getStartDate()) : null)
                .endDate(dto.getEndDate())
                .usageLimit(dto.getUsageLimit())
                .usedCount(dto.getUsedCount())
                .status(dto.getStatus())
                .build();

        // Đảm bảo các trường mặc định khi tạo mới
        if (voucher.getId() == null) {
            voucher.setUsedCount(0);
            if (voucher.getUsageLimit() == null) voucher.setUsageLimit(100);
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

        // Kiểm tra trùng CODE (trừ chính nó)
        if (voucherRepository.existsByCodeIgnoreCase(dto.getCode())) {
            if (dto.getId() == null || !voucherRepository.findById(dto.getId()).orElse(new Voucher()).getCode().equalsIgnoreCase(dto.getCode())) {
                throw new AppException("Mã Voucher đã tồn tại: " + dto.getCode());
            }
        }

        // Kiểm tra logic ngày tháng (mặc dù đã có JS/DTO validation)
        if (dto.getStartDate() != null && dto.getEndDate() != null && dto.getEndDate().isBefore(dto.getStartDate())) {
            throw new AppException("Ngày kết thúc không được nhỏ hơn ngày bắt đầu.");
        }

        Voucher voucherToSave;
        if (dto.getId() == null) {
            // CREATE
            voucherToSave = mapToEntity(dto);
        } else {
            // UPDATE: Giữ lại usedCount và các giá trị mặc định khác
            Voucher existing = voucherRepository.findById(dto.getId())
                    .orElseThrow(() -> new AppException("Voucher không tìm thấy để cập nhật: " + dto.getId()));

            voucherToSave = mapToEntity(dto);
            voucherToSave.setUsedCount(existing.getUsedCount());
        }

        Voucher savedVoucher = voucherRepository.save(voucherToSave);
        return mapToDTO(savedVoucher);
    }

    // 4. DELETE: Xóa Voucher (Có thể là soft delete - đổi status, nhưng ta dùng hard delete)
    @Transactional
    public void deleteVoucher(Integer id) {
        if (!voucherRepository.existsById(id)) {
            throw new AppException("Voucher không tìm thấy để xóa.");
        }
        // TODO: Nên có logic kiểm tra voucher này đã được sử dụng trong Order nào chưa.
        voucherRepository.deleteById(id);
    }
}