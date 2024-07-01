package com.company.carpooling.security.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.servlet.util.matcher.MvcRequestMatcher;
import org.springframework.web.servlet.handler.HandlerMappingIntrospector;


@Configuration
@EnableWebSecurity
public class WebSecurityConfig {

    @Bean
    MvcRequestMatcher.Builder mvc(HandlerMappingIntrospector introspector) {
        return new MvcRequestMatcher.Builder(introspector);
    }

    @Bean
    SecurityFilterChain defaultSecurityFilterChain(
            HttpSecurity http, MvcRequestMatcher.Builder mvc) throws Exception {

        // requires all communications to be over tls since webauthn does not work if http is used
        http.requiresChannel().anyRequest().requiresSecure();

        // configure url authorization rules
        http.authorizeHttpRequests().requestMatchers(
                        mvc.pattern("/"),
                        mvc.pattern("/auth/register"),
                        mvc.pattern("/auth/login"))
                .permitAll()
                .requestMatchers("/images/**", "/fonts/**", "/css/**", "/js/**")
                .permitAll()
                .anyRequest().authenticated();


        return http.build();
    }


}
