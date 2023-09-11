package org.sbsplus.cummunity.service;

import org.sbsplus.cummunity.dto.ArticleDto;
import org.sbsplus.cummunity.entity.Article;
import org.sbsplus.type.Category;
import org.springframework.data.domain.Page;

public interface ArticleService {
    
    Page<ArticleDto> findByCategory(int page, Category category);
    
    ArticleDto findById(Integer articleId);
    
    void increaseHit(Integer articleId);
    
    void save(ArticleDto articleDto);
}
