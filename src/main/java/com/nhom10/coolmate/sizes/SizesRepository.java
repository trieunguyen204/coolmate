package com.nhom10.coolmate.sizes;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SizesRepository extends JpaRepository<Sizes, Integer> {

    boolean existsBySizeNameIgnoreCase(String sizeName);
}