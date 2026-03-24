package com.example.pos_system_backend.repository;

import com.example.pos_system_backend.model.Permission;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface PermissionRepository extends JpaRepository<Permission, Long> {
    List<Permission> findByModule(String module);
}
