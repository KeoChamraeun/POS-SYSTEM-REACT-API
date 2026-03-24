package com.example.pos_system_backend.controller;

import com.example.pos_system_backend.model.Permission;
import com.example.pos_system_backend.model.Role;
import com.example.pos_system_backend.repository.PermissionRepository;
import com.example.pos_system_backend.repository.RoleRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/roles")
@CrossOrigin(origins = "*")
public class RoleController {

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;

    public RoleController(RoleRepository roleRepository, PermissionRepository permissionRepository) {
        this.roleRepository = roleRepository;
        this.permissionRepository = permissionRepository;
    }

    @GetMapping
    public ResponseEntity<List<Role>> getAll() {
        return ResponseEntity.ok(roleRepository.findAll());
    }

    @GetMapping("/permissions")
    public ResponseEntity<List<Permission>> getAllPermissions() {
        return ResponseEntity.ok(permissionRepository.findAll());
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody Map<String, Object> body) {
        String name = ((String) body.get("name")).toUpperCase();
        if (roleRepository.existsByName(name)) {
            return ResponseEntity.badRequest().body(Map.of("message", "Role already exists"));
        }
        Role role = new Role();
        role.setName(name);
        role.setDescription((String) body.get("description"));

        if (body.get("permissionIds") != null) {
            List<Long> ids = ((List<?>) body.get("permissionIds")).stream()
                .map(o -> Long.valueOf(o.toString())).collect(Collectors.toList());
            Set<Permission> perms = new HashSet<>(permissionRepository.findAllById(ids));
            role.setPermissions(perms);
        }
        return ResponseEntity.ok(roleRepository.save(role));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        Role role = roleRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Role not found"));

        if (body.get("name") != null) role.setName(((String) body.get("name")).toUpperCase());
        if (body.get("description") != null) role.setDescription((String) body.get("description"));

        if (body.get("permissionIds") != null) {
            List<Long> ids = ((List<?>) body.get("permissionIds")).stream()
                .map(o -> Long.valueOf(o.toString())).collect(Collectors.toList());
            Set<Permission> perms = new HashSet<>(permissionRepository.findAllById(ids));
            role.setPermissions(perms);
        }
        return ResponseEntity.ok(roleRepository.save(role));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        roleRepository.deleteById(id);
        return ResponseEntity.ok(Map.of("message", "Role deleted"));
    }
}
