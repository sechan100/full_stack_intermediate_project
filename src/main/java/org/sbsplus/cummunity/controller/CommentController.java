package org.sbsplus.cummunity.controller;


import lombok.RequiredArgsConstructor;
import org.sbsplus.cummunity.dto.CommentDto;
import org.sbsplus.cummunity.service.ArticleService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequiredArgsConstructor
public class CommentController {
    
    private final ArticleService articleService;
    
    
    @PostMapping("/comment/write")
    public String commentWrite
            ( @RequestParam Integer articleId
            , @RequestParam(required = false) Integer commentId
            , CommentDto commentDto){
        
        
        // 아이디 여부에 따라서 서비스 로직에서 분기
        commentDto.setId(commentId);
        articleService.saveComment(articleId, commentDto);
        
        
        
        return "redirect:/article/" + articleId;
    }

    @GetMapping("/comment/delete")
    public String deleteComment(@RequestParam Integer articleId, @RequestParam Integer commentId){
        
        articleService.deleteComment(articleId, commentId);
        
        return "redirect:/article/" + articleId;
    }
    
    
}



















