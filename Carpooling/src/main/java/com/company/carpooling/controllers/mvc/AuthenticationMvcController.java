package com.company.carpooling.controllers.mvc;

import com.company.carpooling.exceptions.AuthorizationException;
import com.company.carpooling.exceptions.EntityDuplicateException;
import com.company.carpooling.exceptions.WrongActivationCodeException;
import com.company.carpooling.helpers.mappers.AuthenticationHelper;
import com.company.carpooling.helpers.mappers.UserMapper;
import com.company.carpooling.models.User;
import com.company.carpooling.models.ActivationCode;
import com.company.carpooling.models.dtos.LoginDto;
import com.company.carpooling.models.dtos.RegisterDto;
import com.company.carpooling.services.contracts.UserService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.*;
import java.util.HashMap;
import java.util.Map;
import org.springframework.http.ResponseEntity;

@AllArgsConstructor
@Controller
@RequestMapping("/auth")
public class AuthenticationMvcController {
    public static final String PASSWORD_CONFIRMATION_ERROR = "Password confirmation should match password.";
    private final UserService userService;
    private final AuthenticationHelper authenticationHelper;
    private final UserMapper userMapper;

    @ModelAttribute("isAuthenticated")
    public boolean populateIsAuthenticated(HttpSession session) {
        return session.getAttribute("currentUser") != null;
    }

    @GetMapping("/login")
    public String showLoginPage(Model model) {
        model.addAttribute("login", new LoginDto());
        return "LoginView";
    }

    @PostMapping("/login")
    public String handleLogin(@Valid @ModelAttribute("login") LoginDto loginDto,
                              BindingResult bindingResult,
                              HttpSession session) {

        if (bindingResult.hasErrors()) {
            for (FieldError error : bindingResult.getFieldErrors()) {
                System.out.println("Message: " + error.getDefaultMessage());
            }
            return "LoginView";
        }

        try {
            User user = authenticationHelper.verifyAuthentication(loginDto.getEmail(), loginDto.getPassword());
            session.setAttribute("currentUser", loginDto.getEmail());
            session.setAttribute("isAdmin", user.isAdmin());
            return "redirect:/";

        } catch (AuthorizationException e) {
            bindingResult.rejectValue("email", "auth_error", e.getMessage());
            return "LoginView";
        }
    }

    @GetMapping("/logout")
    public String handleLogout(HttpSession session) {
        session.removeAttribute("currentUser");
        return "redirect:/auth/login";
    }

    @GetMapping("/register")
    public String showRegisterPage () {
        return "RegisterView";
    }

    @PostMapping("/register")

    public ResponseEntity<Map<String, String>> handleRegister(@Valid @RequestBody RegisterDto register,
                                 BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            Map<String, String> errors = new HashMap<>();
            if(!register.getPassword().equals(register.getPasswordConfirm())) {
                bindingResult.rejectValue("passwordConfirm", "password_error", PASSWORD_CONFIRMATION_ERROR);
            }
            for (FieldError error : bindingResult.getFieldErrors()) {
                errors.put(error.getField(), error.getDefaultMessage());
            }
            return ResponseEntity.badRequest().body(errors);
        }

        try {
            User user = userMapper.fromDtoRegister(register);
            userService.create(user);
            authenticationHelper.verifyAuthentication(register.getEmail(), register.getPassword());
            return ResponseEntity.ok().body(Map.of("message", "Registration successful"));
        } catch (EntityDuplicateException e) {
            bindingResult.rejectValue("username", "username_error", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("message","Username exists already!"));
        }
    }

    @GetMapping("/verify")
    public String showEmailConfirmationPage (Model model) {
        model.addAttribute("email", new ActivationCode());
        return "EmailConfirmationView";
    }

    @PostMapping("/verify")
    public String handleEmailConfirmation(@Valid @ModelAttribute("email") ActivationCode activationCode,
                                 BindingResult bindingResult) {
        if(bindingResult.hasErrors()) {
            return "EmailConfirmationView";
        }
        try {
            userService.activateAccount(activationCode.getActivationCode());
            return "redirect:/auth/login";
        } catch (WrongActivationCodeException e) {
            bindingResult.rejectValue("code", "code_error", e.getMessage());
            return "EmailConfirmationView";
        }
    }

    @PostMapping("/verify/new")
    public String handleNewCode(@Valid @ModelAttribute("email") ActivationCode activationCode,
                                BindingResult bindingResult) {
        if(bindingResult.hasErrors()) {
            return "EmailConfirmationView";
        }
        try {
            User user = userService.getByEmail(activationCode.getEmail());
            userService.resendActivationCode(user.getUsername());
            return "redirect:/auth/verify";
        } catch (WrongActivationCodeException e) {
            bindingResult.rejectValue("code", "code_error", e.getMessage());
            return "EmailConfirmationView";
        }
    }
}
