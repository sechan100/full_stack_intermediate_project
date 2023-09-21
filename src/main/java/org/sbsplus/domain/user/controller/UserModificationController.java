package org.sbsplus.domain.user.controller;


import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.sbsplus.domain.user.dto.UserDto;
import org.sbsplus.domain.user.entity.User;
import org.sbsplus.domain.user.service.UserService;
import org.sbsplus.general.security.principal.UserContext;
import org.sbsplus.general.type.Category;
import org.sbsplus.util.Rq;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
@RequiredArgsConstructor
public class UserModificationController {
    
    private final UserDetailsService userDetailsService;
    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    
    @Qualifier("userToDto")
    private final ModelMapper mapper;
    private final Rq rq;

    @GetMapping("/{username}/modification")
    public String modification(@PathVariable("username") String username, HttpServletRequest request, Model model){

        // url로 요청한 username과 session의 User principal의 username이 동일하지 않은 경우..
        if(!username.equals(rq.getUser().getUsername())){
            throw new AccessDeniedException("해당 계정에 대한 수정 권한이 없습니다.");
        }
        
        UserDto userDto = mapper.map(rq.getUser(), UserDto.class);
        
        // Dto
        model.addAttribute("user", userDto);
        
        // 수업 유형 리스트
        model.addAttribute("categories", Category.getCategories());

        return "/user/modification";
    }
    

    // modification process
    @PostMapping("/modification")
    @Transactional
    public String modificationPrcs(UserDto userDto) {

        // User Entity for dirty checking
        User user = userService.findByUsername(userDto.getUsername());
        
        // password 칸에 빈 문자열이 오지 않았을 경우..
        if(!userDto.getPassword().isEmpty()){
            
            // password confirm
            if(userService.confirmPassword(userDto.getPassword(), userDto.getConfirmPassword())) {
                return String.format("redirect:/%s/modification?error=true&type=password", rq.getUser().getUsername());
            } else {
                // password encode and modify entity
                user.setPassword(passwordEncoder.encode(userDto.getPassword()));
            }
            
        }
       
        
        /*
         *************************
         * name 변동 체크
         *************************
         */
        if(!user.getName().equals(userDto.getName())) {
            // Entity 수정
            user.setName(userDto.getName());
        }
        
        /*
         *************************
         * nickname 변동 체크
         *************************
         */
        if(!user.getNickname().equals(userDto.getNickname())) {
            // Entity 수정
            user.setNickname(userDto.getNickname());
        }
        
        
        /*
         *************************
         * category 변동 체크
         *************************
         */
        if(!user.getCategory().getValue().equals(userDto.getCategory())) {
            
            // Entity 수정
            user.setCategory(Category.convertStringToEnum(userDto.getCategory()));
        }
        
        
        UserContext accountContext = (UserContext) userDetailsService.loadUserByUsername(user.getUsername());
        
        UsernamePasswordAuthenticationToken newAuthentication = new UsernamePasswordAuthenticationToken(
                user, null, accountContext.getAuthorities()
        );
        
        SecurityContextHolder.getContext().setAuthentication(newAuthentication);

        return String.format("redirect:/%s/modification", rq.getUser().getUsername());
    }

}



















