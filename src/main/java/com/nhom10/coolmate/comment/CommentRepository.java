package com.nhom10.coolmate.comment;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Integer> {


    List<Comment> findAllByProductIdOrderByCreatedAtDesc(Integer productId);


    List<Comment> findByProductIdAndParentIsNullOrderByCreatedAtDesc(Integer productId);

    List<Comment> findAllByOrderByCreatedAtDesc();
}