package org.sbsplus.user.service.validation;

import org.sbsplus.user.dto.AccountDto;
import org.sbsplus.user.entity.Account;

public interface AccountService {

    Account findById(Integer id);

    int countByUsername(String username);

    int countByEmail(String email);

    int countByPhone(String phone);

    // 이미 존재하는 username 확인
    boolean isValidUsername(String username);

    // 이미 존재하는 이메일 확인
    boolean isValidEmail(String email);

    // 이미 존재하는 전화번호 확인
    boolean isValidPhone(String phone);

    // 비밀번호 확인이 일치하는가
    boolean confirmPassword(String password, String confirmPassword);

    // 전화번호 포멧이 올바른가
    boolean isVaildPhoneFormat(String phone);
    

    /**
     * convert Dto object to Entity with configured PasswordEncoder Bean wired RegisterService
     * @param accountDto Dto Object
     * @param role USER, ADMIN etc..
     * @return Account Entity
     */
    Account convertToEntityWithRole(AccountDto accountDto, String role);

    void save(Account account);

}