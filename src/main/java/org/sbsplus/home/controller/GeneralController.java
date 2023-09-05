package org.sbsplus.home.controller;

import lombok.RequiredArgsConstructor;
import org.sbsplus.util.Rq;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;


@Controller
@RequiredArgsConstructor
public class GeneralController {

    private final Rq rq;

    @GetMapping("/")
    public String home(){
        return "/home";
    }


    @GetMapping("/access_denied")
    public String accessDenied(Model model){

        String msg = (String) rq.getRequest().getAttribute("msg");

        model.addAttribute("msg", msg);

        return "/util/access_denied";
    }


}