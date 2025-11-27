package com.nhom10.coolmate.config;

import com.nhom10.coolmate.user.UserService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.password.NoOpPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.beans.factory.annotation.Autowired;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Autowired
    private AuthenticationSuccessHandler customAuthenticationSuccessHandler;

    @SuppressWarnings("deprecation")
    @Bean
    public PasswordEncoder passwordEncoder() {
        // LƯU Ý: Đang sử dụng NoOpPasswordEncoder (không mã hóa) cho mục đích demo/học tập.
        // Trong môi trường thực tế, phải sử dụng BCryptPasswordEncoder.
        return NoOpPasswordEncoder.getInstance();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider(UserService userService) {
        DaoAuthenticationProvider auth = new DaoAuthenticationProvider();
        auth.setUserDetailsService(userService);
        auth.setPasswordEncoder(passwordEncoder());
        return auth;
    }

    @Bean
    // Đã thêm UserService vào tham số
    public SecurityFilterChain filterChain(HttpSecurity http, DaoAuthenticationProvider authenticationProvider, UserService userService) throws Exception {
        http
                .authorizeHttpRequests(authorize -> authorize
                        // Cho phép truy cập không cần đăng nhập vào các trang công khai
                        .requestMatchers("/login", "/register", "/css/**", "/js/**", "/webjars/**").permitAll()

                        // BẮT BUỘC đăng nhập VÀ PHẢI CÓ ROLE_ADMIN cho tất cả các đường dẫn /admin/**
                        .requestMatchers("/admin/**").hasRole("ADMIN")

                        // Các request khác bắt buộc phải đăng nhập (USER và ADMIN)
                        .anyRequest().authenticated()
                )
                .formLogin(form -> form
                        .loginPage("/login")
                        .loginProcessingUrl("/login")
                        .successHandler(customAuthenticationSuccessHandler)
                        .failureUrl("/login?error")
                        .permitAll()
                )
                .logout(logout -> logout
                        .logoutSuccessUrl("/login?logout")
                        .permitAll()
                )
                // Đã sửa lỗi: Dùng trực tiếp userService thay vì authenticationProvider.getUserDetailsService()
                .rememberMe(rememberMe -> rememberMe
                        .key("CoolmateSecretKey123")
                        .tokenValiditySeconds(86400)
                        .userDetailsService(userService)
                )
                .authenticationProvider(authenticationProvider);

        return http.build();
    }
}