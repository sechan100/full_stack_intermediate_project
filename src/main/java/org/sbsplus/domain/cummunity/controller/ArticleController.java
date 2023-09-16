package org.sbsplus.domain.cummunity.controller;


import jakarta.servlet.http.Cookie;
import lombok.RequiredArgsConstructor;
import org.sbsplus.domain.cummunity.dto.ArticleDto;
import org.sbsplus.domain.cummunity.service.ArticleService;
import org.sbsplus.general.type.Category;
import org.sbsplus.util.Pager;
import org.sbsplus.util.Rq;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.AccessDeniedException;


import java.util.List;
import java.util.Objects;

@Controller
@RequiredArgsConstructor
public class ArticleController {
    
    private final ArticleService articleService;
    private final Rq rq;
    
    
    // 게시글 리스트
    @GetMapping("/article")
    public String articleList(
              @RequestParam(defaultValue = "1") Integer page
            , @RequestParam(defaultValue = "ALL", name = "category") String category_
            , @RequestParam(required = false) String searchMatcher
            , Model model
            ){
        
        Category category = Category.convertStringToEnum(category_);
        
        Page<ArticleDto> articles = null;
        
        // 검색 X
        if(searchMatcher == null) {
            
            articles = articleService.findByCategory(page - 1, category);
            
        // 검색 O
        } else {
            
            articles = articleService.findBySearchMatcher(page - 1, category, searchMatcher);
            
        }
        
        Integer totalPage = articles.getTotalPages();
        
        if(page > articles.getTotalPages() && totalPage != 0) {
            
            return rq.unexpectedRequestForWardUri("존재하지 않는 페이지입니다.");
            
        }
        
        
        
        model.addAttribute("articles", articles);
        
        List<Category> categories = Category.getCategories();
        model.addAttribute("categories", categories);
        
        List<Integer> pageRange = Pager.getPageRange(page, totalPage);
        model.addAttribute("pageRange", pageRange);
        
        model.addAttribute("currentPage", page);
        
        return "/article/articleList";
    }
    
    
    // 게시글 디테일 조회
    @GetMapping("/article/{articleId}")
    public String articleDetail(@PathVariable Integer articleId, Model model){
        
        // 중복 조회수 카운팅 방지
        Cookie oldCookie = rq.getCookie("viewedArticles");
        if (oldCookie != null) {
            if (!oldCookie.getValue().contains("[" + articleId.toString() + "]")) {
                articleService.increaseHit(articleId);
                oldCookie.setValue(oldCookie.getValue() + "_[" + articleId + "]");
                oldCookie.setPath("/article");
                oldCookie.setMaxAge(60 * 60 * 24);
                rq.getResponse().addCookie(oldCookie);
            }
        } else {
            articleService.increaseHit(articleId);
            Cookie newCookie = new Cookie("viewedArticles","[" + articleId + "]");
            newCookie.setPath("/article");
            newCookie.setMaxAge(60 * 60 * 24);
            rq.getResponse().addCookie(newCookie);
        }
        
        
        ArticleDto article = articleService.findById(articleId);
        model.addAttribute("article", article);
        
        return "/article/articleDetail";
    }
    
    // 게시글 작성/수정 페이지 폼
    @GetMapping("/article/write")
    public String articleWriteForm(Model model, @RequestParam(required = false) Integer id) throws AccessDeniedException {
        
        // 새로운 글 작성
        ArticleDto articleDto;
        if(id == null) {
            
            articleDto = new ArticleDto();
            
        // 기존 글 수정
        } else {
            if(articleService.checkArticleOwnership(id)){
                articleDto = articleService.findById(id);
            } else {
                throw new AccessDeniedException("해당 게시물에 대한 수정권한이 존재하지 않습니다.");
            }
        }
        model.addAttribute("article", articleDto);
        model.addAttribute("categories", Category.getCategories());
        
        return "/article/writeForm";
    }
    
    // 게시글 작성 프로세스
    @PostMapping("/article/write")
    public String articleWritePrcs(ArticleDto articleDto, @RequestParam(required = false) Integer id){
        
        articleService.save(articleDto);
        
        return "redirect:/article?page=1&category=" + articleDto.getCategory().getValue();
    }
    
    // 게시글 삭제 프로세스
    @GetMapping("/article/delete")
    public String articleDelete(@RequestParam Integer id) throws AccessDeniedException {
        
        articleService.delete(id);
    
        return "redirect:/article?page=1&category=ALL";
    }
    
    // 게시글 좋아요
    @GetMapping("/article/like")
    public String articleLike(@RequestParam Integer id){
        
        // 기존 추천 여부 확인
        if(articleService.hasUserLiked(id)){
            
            // 기존 추천 여부가 있다면 추천 취소
            articleService.unlikeArticle(id);
            
        } else {
            
            // 추천한 적이 없다면 추천 생성
            articleService.likeArticle(id);
        }
        
        
        
        return "redirect:/article/" + id;
    }
    
    @GetMapping("/ajax/article/like")
    @ResponseBody
    public String ajaxArticleLike(@RequestParam Integer id){
        
        // 기존 추천 여부 확인
        if(articleService.hasUserLiked(id)){
            
            // 기존 추천 여부가 있다면 추천 취소
            articleService.unlikeArticle(id);
            return String.format("""
                 <a like-bok class="no-underline text-grey-darker" th:hx-get="@{/ajax/article/like(id=${article.id})}">
                     <span class="">%s</span>
                     <i like-icon class="animate__heartBeat fa-regular fa-heart"></i>
                 </a>
                """, articleService.findById(id).getLikes().size());
            
        } else {
            
            // 추천한 적이 없다면 추천 생성
            articleService.likeArticle(id);
            return String.format("""
                 <a like-bok class="no-underline text-grey-darker" th:hx-get="@{/ajax/article/like(id=${article.id})}">
                     <span class="">%s</span>
                     <i like-icon class="animate__heartBeat fa fa-heart"></i>
                 </a>
                """, articleService.findById(id).getLikes().size());
        }
    }
}














