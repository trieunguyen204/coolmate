package com.nhom10.coolmate.user;


import lombok.Builder;
import lombok.Data;
import java.sql.Timestamp;

@Data
@Builder
public class UserDTO {
    private Integer id;
    private String fullName;
    private String email;
    private String phone;
    private Role role;
    private Integer status;
    private Timestamp createdAt;
    private Gender gender;


    public boolean isEnabled() {
        return this.status != null && this.status == 1;
    }


}