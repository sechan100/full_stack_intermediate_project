package org.sbsplus.user.controller;


import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.sbsplus.user.entity.User;
import org.sbsplus.user.repository.UserRepository;
import org.sbsplus.util.Rq;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
@Slf4j
@RequiredArgsConstructor
public class UserWithdrawalController {
    
    private final Rq rq;
    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;
    
    
    @GetMapping("/withdrawal")
    public String withdrawal(){
        
        return "/user/withdrawal";
    }
    
    @PostMapping("/withdrawal")
    public String withdrawal(String password){
        
        User withdrawalTargetUser = rq.getUser();
        String encodedLoginedUserPassword = rq.getUser().getPassword();
        
        
        // password incorrect
        if(!passwordEncoder.matches(password, encodedLoginedUserPassword)){
            throw new BadCredentialsException("BadCredentialsException");
        }
        
        // force logout
        HttpSession session = rq.getSession();
        
        // 1) AuthenticationToken: UsernamePasswordAuthenticationToken initialization to null
        SecurityContextHolder.getContext().setAuthentication(null);
        
        // 2) session expire
        session.invalidate();
        
        // delete on DB
        userRepository.delete(withdrawalTargetUser);
        
        
        return "redirect:/";
    }
}