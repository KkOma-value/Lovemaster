package org.example.springai_learn.auth.repository;

import org.example.springai_learn.auth.entity.UserImage;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface UserImageRepository extends JpaRepository<UserImage, String> {
    List<UserImage> findByUserId(String userId);
    List<UserImage> findByUserIdAndImageType(String userId, String imageType);
}
