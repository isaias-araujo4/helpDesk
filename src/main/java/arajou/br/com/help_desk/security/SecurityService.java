package arajou.br.com.help_desk.security;

import arajou.br.com.help_desk.user.model.UserModel;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class SecurityService {

    public UserModel getAuthenticatedUser(){
        return (UserModel) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }
}
