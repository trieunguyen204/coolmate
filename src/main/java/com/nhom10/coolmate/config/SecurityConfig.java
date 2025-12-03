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
    public SecurityFilterChain filterChain(HttpSecurity http, DaoAuthenticationProvider authenticationProvider, UserService userService) throws Exception {
        http
                .authorizeHttpRequests(authorize -> authorize

                        // 1. PUBLIC: Cho phép truy cập công cộng (Login, Register, Static)
                        .requestMatchers(
                                "/login",
                                "/register",
                                "/css/**", "/js/**", "/webjars/**", "/uploads/**",
                                "/",
                                "/user/product",
                                "/user/contact",
                                "/user/about"
                                ,"/user/home"




                        ).permitAll()

                        // 2. PHÂN QUYỀN ADMIN: Chỉ ROLE_ADMIN truy cập /admin/**
                        .requestMatchers("/admin/**").hasRole("ADMIN")

                        // 4. BẤT KỲ CÁI CÒN LẠI: Yêu cầu xác thực (thường là không cần nếu đã bao phủ hết)
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
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/login?logout")
                        .invalidateHttpSession(true)
                        .deleteCookies("JSESSIONID", "remember-me")
                        .permitAll()
                )
                .rememberMe(rememberMe -> rememberMe
                        .key("CoolmateSecretKey123")
                        .tokenValiditySeconds(86400)
                        .userDetailsService(userService)
                )
                .authenticationProvider(authenticationProvider);

        return http.build();
    }
}