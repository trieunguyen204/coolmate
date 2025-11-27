package com.nhom10.coolmate.sizes;

import com.nhom10.coolmate.exception.ResourceNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class SizesService {

    @Autowired
    private SizesRepository sizesRepository;

    public List<Sizes> getAllSizes() {
        return sizesRepository.findAll();
    }

    public Sizes getSizesById(Integer id) {
        return sizesRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Size", "ID", id));
    }

    public void saveSizes(Sizes sizes) {
        // Kiểm tra trùng tên
        Optional<Sizes> existingSizes = sizesRepository.findBySizeNameIgnoreCase(sizes.getSizeName());

        if (existingSizes.isPresent()) {
            // Nếu trùng tên và ID khác nhau (hoặc thêm mới)
            if (sizes.getId() == null || !existingSizes.get().getId().equals(sizes.getId())) {
                throw new IllegalArgumentException("Tên size '" + sizes.getSizeName() + "' đã tồn tại!");
            }
        }

        // Viết hoa tên size (ví dụ: xl -> XL)
        sizes.setSizeName(sizes.getSizeName().toUpperCase());

        sizesRepository.save(sizes);
    }

    public void deleteSizes(Integer id) {
        if (!sizesRepository.existsById(id)) {
            throw new ResourceNotFoundException("Size", "ID", id);
        }
        try {
            sizesRepository.deleteById(id);
        } catch (DataIntegrityViolationException e) {
            throw new IllegalStateException("Không thể xóa Size này vì đang được sử dụng trong sản phẩm.");
        }
    }
}