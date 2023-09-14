package org.sbsplus.cummunity.service;


import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.sbsplus.admin.AdminService;
import org.sbsplus.cummunity.dto.ArticleDto;
import org.sbsplus.cummunity.dto.CommentDto;
import org.sbsplus.cummunity.entity.Article;
import org.sbsplus.cummunity.entity.Comment;
import org.sbsplus.cummunity.entity.like.ArticleLike;
import org.sbsplus.cummunity.entity.like.CommentLike;
import org.sbsplus.cummunity.entity.like.Like;
import org.sbsplus.cummunity.repository.ArticleRepository;
import org.sbsplus.type.Category;
import org.sbsplus.user.entity.User;
import org.sbsplus.user.service.UserService;
import org.sbsplus.util.Rq;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;


@Service
@RequiredArgsConstructor
public class ArticleServiceImpl implements ArticleService {
    
    private final ArticleRepository articleRepository;
    private final AdminService adminService;
    private final UserService userService;
    private final Rq rq;
    
    @Override
    public Page<ArticleDto> findByCategory(int page, Category category){
        
        try {
            Page<Article> articles_ = null;
            
            Pageable pageRequest = PageRequest.of(page, 20, Sort.by(Sort.Direction.DESC, "createAt"));
            
            if(category == Category.ALL) {
                articles_ = articleRepository.findAll(pageRequest);
            } else {
                articles_ = articleRepository.findByCategory(pageRequest, category);
            }
            // N + 1 최적화 필요.
            return articles_.map(article -> (new ModelMapper()).map(article, ArticleDto.class));
            
        } catch(IllegalArgumentException e){
            
            return null;
        }
    }
    
    @Override
    public ArticleDto findById(Integer articleId) {
        
        Article article = articleRepository.findById(articleId).orElseThrow(() -> new EntityNotFoundException("게시글이 존재하지 않습니다."));
        
        return (new ModelMapper()).map(article, ArticleDto.class);
    }
    
    @Override
    @Transactional
    public void increaseHit(Integer articleId) {
        Article article = articleRepository.findById(articleId).orElseThrow(() -> new EntityNotFoundException("게시글이 존재하지 않습니다."));
        article.increaseHit();
    }
    
    @Override
    @Transactional
    public void save(ArticleDto articleDto) {
        
        // 새로운 글 작성
        if(articleDto.getId() == null){
            
            Article article = (new ModelMapper()).map(articleDto, Article.class);
            
            if(article.getUser() == null){
                article.setUser(rq.getUser());
            }
            
            articleRepository.save(article);
            
        // 기존 글 수정
        } else {
            if(checkArticleOwnership(articleDto.getId())) {
                Article article = articleRepository.findById(articleDto.getId()).orElseThrow();
                    article.setCategory(articleDto.getCategory());
                    article.setTitle(articleDto.getTitle());
                    article.setContent(articleDto.getContent());
            }
        }
        
    }
    
    
    @Override
    @Transactional
    public boolean checkArticleOwnership(Integer articleId){
        
        Article article = articleRepository.findById(articleId).orElseThrow();
        
        
        // admin도 수정은 못함
        if(rq.getUser() == null){
            return false;
        }
        
        
        
        User user_ = rq.getUser();
        User user = userService.findByUsername(user_.getUsername());
        
        return article.getUser() == user;
    }
    
    @Override
    public void delete(Integer articleId) throws AccessDeniedException {
        
        // 수정은 소유자만 가능, 삭제는 admin도 가능.
        if(checkArticleOwnership(articleId) || adminService.isAdmin()){
            articleRepository.deleteById(articleId);
        } else {
            throw new AccessDeniedException("게시물에 대한 접근 권한이 없습니다.");
        }
    }
    
    @Override
    public boolean hasUserLiked(Integer articleId) {
        
        Article article = articleRepository.findById(articleId).orElseThrow();
        List<Like> likes = article.getLikes();
        
        for(Like like : likes){
            // likes 중에 하나라도 로그인중인 유저가 누른 like가 존재하는 경우
            if(like.getUser().equals(rq.getUser())){
                return true;
            }
        }
        
        // likes 중에 로그인중인 유저 소유의 like가 존재하지 않는 경우
        return false;
    }
    
    @Override
    @Transactional
    public void likeArticle(Integer articleId) {
        Article article = articleRepository.findById(articleId).orElseThrow();
        article.getLikes().add(new ArticleLike(rq.getUser()));
    }
    
    @Override
    @Transactional
    public void unlikeArticle(Integer articleId) {
        Article article = articleRepository.findById(articleId).orElseThrow();
        List<Like> likes = article.getLikes();
        Like removeTargetLike = null;
        
        for(Like like : likes){
            if(like.getUser().equals(rq.getUser())){
                removeTargetLike = like;
            }
        }
        
        if(removeTargetLike != null){
            article.getLikes().remove(removeTargetLike);
        } else {
            throw new EntityNotFoundException("삭제할 엔티티(Like)를 특정하지 못했습니다.");
        }
        
        // 왜 변경감지가 안되지?
        articleRepository.save(article);
    }
    
    @Override
    @Transactional
    public void saveComment(Integer articleId, CommentDto commentDto) {
        
        
        // 새로운 댓글 등록
        if(commentDto.getId() == null){
            
            // user 세팅
            commentDto.setUser(rq.getUser());
            
            // 엔티티와 매핑
            Comment comment = (new ModelMapper()).map(commentDto, Comment.class);
            
            // 게시글 엔티티를 영속성 컨텍스트 1차 캐쉬에 저장
            Article article = articleRepository.findById(articleId).orElseThrow();
            List<Comment> comments = article.getComments();
            
            // comment 저장
            comments.add(comment);
            
        // 기존 댓글 수정
        } else {
            
            // 댓글의 소유권이 확인된 경우
            if(checkCommentOwnership(articleId, commentDto.getId())){
                
                List<Comment> comments = articleRepository.findById(articleId).orElseThrow().getComments();
                
                for(Comment comment : comments){
                    if(Objects.equals(comment.getId(), commentDto.getId())){
                        comment.setContent(commentDto.getContent());
                    }
                }
                
            }
            
        }
    }
    
    @Override
    @Transactional
    public void deleteComment(Integer articleId, Integer commentId) {
        
        // 권한 확인 (작성자 or 관리자)
        if(
                adminService.isAdmin()
                ||
                checkCommentOwnership(articleId, commentId)
        ){
            
            List<Comment> comments = articleRepository.findById(articleId).orElseThrow().getComments();
            
            // 삭제
            comments.removeIf(comment -> Objects.equals(comment.getId(), commentId));
            
        }
    }
    
    @Override
    public boolean checkCommentOwnership(Integer articleId, Integer commentId) {
        
        Article article = articleRepository.findById(articleId).orElseThrow();
        List<Comment> comments = article.getComments();
        
        for(Comment comment : comments){
            
            // DB에서 가져온 대상 댓글의 id와 dto로 받은 댓글 id가 일치하는 경우
            if(Objects.equals(comment.getId(), commentId)){
                
                // 현재 로그인 중인 사용자의 소유인 댓글이 맞는 경우
                if(comment.getUser().equals(rq.getUser())){
                    
                    return true;
                    
                } else {
                    throw new EntityNotFoundException("댓글 수정권한이 없습니다.");
                }
                
            }
        }
        
        return false;
    }
    
    @Override
    public boolean hasUserLikedComment(Integer articleId, Integer commentId) {
        
        // 게시글에서 댓글 정보 가져와서 매칭
        Article article = articleRepository.findById(articleId).orElseThrow();
        List<Comment> comments = article.getComments();
        
        // 댓글 반복
        for(Comment comment : comments){
            
            // 타겟 댓글
            if(Objects.equals(comment.getId(), commentId)){
                
                // 추천 리스트 반복
                List<Like> likes = comment.getLikes();
                for(Like like : likes){
                    if(like.getUser().equals(rq.getUser())){
                        // 매칭 O: 좋아요 기록있음 -> true
                        return true;
                    }
                }
            }
        }
        
        // 매칭 X: 좋아요 없음 -> false
        return false;
    }
    
    @Override
    @Transactional
    public void unlikeComment(Integer articleId, Integer commentId) {
        Article article = articleRepository.findById(articleId).orElseThrow();
        List<Comment> comments = article.getComments();
        
        // 댓글 반복
        for(Comment comment : comments){
            // 타겟 댓글
            if(Objects.equals(comment.getId(), commentId)){
                // 추천 리스트 반복
                List<Like> likes = comment.getLikes();
                for(Like like : likes){
                    if(like.getUser().equals(rq.getUser())){
                        likes.remove(like);
                        return;
                    }
                }
            }
        }
    }
    
    @Override
    @Transactional
    public void likeComment(Integer articleId, Integer commentId) {
        Article article = articleRepository.findById(articleId).orElseThrow();
        List<Comment> comments = article.getComments();
        
        // 댓글 반복
        for(Comment comment : comments){
            // 타겟 댓글
            if(Objects.equals(comment.getId(), commentId)){
                List<Like> likes = comment.getLikes();
                likes.add(new CommentLike(rq.getUser()));
                return;
            }
        }
    }
    
    
}












