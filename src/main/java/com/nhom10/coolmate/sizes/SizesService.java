package com.nhom10.coolmate.sizes;

import com.nhom10.coolmate.exception.AppException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SizesService {

    private final SizesRepository sizesRepository;

    // --- Mapper ---
    private SizeDTO mapToDTO(Sizes size) {
        return SizeDTO.builder()
                .id(size.getId())
                .sizeName(size.getSizeName())
                .build();
    }

    private Sizes mapToEntity(SizeDTO dto) {
        return Sizes.builder()
                .id(dto.getId())
                .sizeName(dto.getSizeName())
                .build();
    }

    // --- 1. READ: Lấy tất cả Sizes ---
    public List<SizeDTO> getAllSizes() {
        return sizesRepository.findAll().stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    // --- 2. READ: Lấy chi tiết Size theo ID ---
    public SizeDTO getSizeById(Integer id) {
        Sizes size = sizesRepository.findById(id)
                .orElseThrow(() -> new AppException("Size không tìm thấy với ID: " + id));
        return mapToDTO(size);
    }

    // --- 3. SAVE: Thêm mới hoặc Cập nhật Size ---
    @Transactional
    public SizeDTO saveSize(SizeDTO dto) {
        // --- 3a. Kiểm tra trùng tên (Create & Update) ---
        if (sizesRepository.existsBySizeNameIgnoreCase(dto.getSizeName())) {
            // Nếu là UPDATE, kiểm tra xem tên trùng có phải của chính nó không
            if (dto.getId() != null) {
                Sizes existingSize = sizesRepository.findById(dto.getId()).orElse(null);
                if (existingSize != null && existingSize.getSizeName().equalsIgnoreCase(dto.getSizeName())) {
                    // Cùng tên và là chính nó -> OK
                } else {
                    throw new AppException("Tên Size đã tồn tại: " + dto.getSizeName());
                }
            } else {
                // Thêm mới và trùng tên -> Lỗi
                throw new AppException("Tên Size đã tồn tại: " + dto.getSizeName());
            }
        }

        Sizes sizeToSave;
        if (dto.getId() == null) {
            // CREATE
            sizeToSave = mapToEntity(dto);
        } else {
            // UPDATE
            sizeToSave = sizesRepository.findById(dto.getId())
                    .orElseThrow(() -> new AppException("Size không tìm thấy với ID: " + dto.getId()));
            sizeToSave.setSizeName(dto.getSizeName());
        }

        Sizes savedSize = sizesRepository.save(sizeToSave);
        return mapToDTO(savedSize);
    }

    // --- 4. DELETE: Xóa Size ---
    @Transactional
    public void deleteSize(Integer id) {
        if (!sizesRepository.existsById(id)) {
            throw new AppException("Size không tìm thấy với ID: " + id);
        }
        // TODO: Cần kiểm tra nếu có ProductVariant nào đang dùng size này thì không cho xóa
        sizesRepository.deleteById(id);
    }
}