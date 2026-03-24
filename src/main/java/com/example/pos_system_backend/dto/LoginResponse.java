package com.example.pos_system_backend.dto;
import java.util.Set;

public class LoginResponse {
    private String token;
    private String username;
    private String fullName;
    private String role;
    private Set<String> permissions;
    private Long branchId;
    private String branchName;

    public LoginResponse() {}
    public String getToken() { return token; }
    public void setToken(String v) { this.token = v; }
    public String getUsername() { return username; }
    public void setUsername(String v) { this.username = v; }
    public String getFullName() { return fullName; }
    public void setFullName(String v) { this.fullName = v; }
    public String getRole() { return role; }
    public void setRole(String v) { this.role = v; }
    public Set<String> getPermissions() { return permissions; }
    public void setPermissions(Set<String> v) { this.permissions = v; }
    public Long getBranchId() { return branchId; }
    public void setBranchId(Long v) { this.branchId = v; }
    public String getBranchName() { return branchName; }
    public void setBranchName(String v) { this.branchName = v; }

    public static LoginResponseBuilder builder() { return new LoginResponseBuilder(); }
    public static class LoginResponseBuilder {
        private final LoginResponse r = new LoginResponse();
        public LoginResponseBuilder token(String v) { r.token=v; return this; }
        public LoginResponseBuilder username(String v) { r.username=v; return this; }
        public LoginResponseBuilder fullName(String v) { r.fullName=v; return this; }
        public LoginResponseBuilder role(String v) { r.role=v; return this; }
        public LoginResponseBuilder permissions(Set<String> v) { r.permissions=v; return this; }
        public LoginResponseBuilder branchId(Long v) { r.branchId=v; return this; }
        public LoginResponseBuilder branchName(String v) { r.branchName=v; return this; }
        public LoginResponse build() { return r; }
    }
}
