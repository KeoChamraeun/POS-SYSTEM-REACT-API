package com.example.pos_system_backend.controller;
import com.example.pos_system_backend.model.Branch;
import com.example.pos_system_backend.repository.BranchRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/branches")
@CrossOrigin(origins = "*")
public class BranchController {
    private final BranchRepository branchRepository;
    public BranchController(BranchRepository branchRepository) {
        this.branchRepository = branchRepository;
    }

    @GetMapping
    public ResponseEntity<List<Branch>> getAll() {
        return ResponseEntity.ok(branchRepository.findAll());
    }

    @PostMapping
    public ResponseEntity<Branch> create(@RequestBody Branch branch) {
        return ResponseEntity.ok(branchRepository.save(branch));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Branch> update(@PathVariable Long id, @RequestBody Branch updated) {
        Branch branch = branchRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Branch not found"));
        branch.setName(updated.getName());
        branch.setAddress(updated.getAddress());
        branch.setPhone(updated.getPhone());
        return ResponseEntity.ok(branchRepository.save(branch));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        branchRepository.deleteById(id);
        return ResponseEntity.ok(Map.of("message", "Branch deleted"));
    }
}
