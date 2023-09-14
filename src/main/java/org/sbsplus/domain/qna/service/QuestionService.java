package org.sbsplus.domain.qna.service;

import lombok.RequiredArgsConstructor;
import org.sbsplus.domain.qna.entity.Question;
import org.sbsplus.domain.qna.repository.QuestionRepository;
import org.sbsplus.domain.qna.DataNotFoundException;
import org.sbsplus.general.type.Category;
import org.sbsplus.util.Rq;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
@Service
public class QuestionService {

    private final QuestionRepository questionRepository;
    private final Rq rq;

    public Page<Question> getList(int page) {
        List<Sort.Order> sorts = new ArrayList<>();
        sorts.add(Sort.Order.desc("createAt"));
        Pageable pageable = PageRequest.of(page, 10, Sort.by(sorts));
        return this.questionRepository.findAll(pageable);
    }
    public Question getQuestion(Integer id) {
        Optional<Question> question = this.questionRepository.findById(id);
        if(question.isPresent()) {
            return question.get();
        } else {
            throw new DataNotFoundException("question not found");
        }
    }

    public void create(String subject, String content, int point, String category) {
       Category c = Category.convertStringToEnum(category);
        Question q = new Question();
        q.setSubject(subject);
        q.setContent(content);
        q.setCategory(c);
        q.setPoint(point);
        q.setUser(rq.getUser());
        this.questionRepository.save(q);
    }

    public void modify(Question question, String subject, String content, Category category) {
        question.setSubject(subject);
        question.setContent(content);
        question.setCategory(category);
        this.questionRepository.save(question);
    }

    public void delete(Question question) {
        this.questionRepository.delete(question);
    }
}