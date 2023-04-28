package com.ll.gramgram.boundedContext.home.controller;

import com.ll.gramgram.base.rq.Rq;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.Enumeration;

@Controller
@RequiredArgsConstructor
public class HomeController {

    private final Rq rq;

    //루트 페이지
    @GetMapping("/")
    public String showMain() {

        //시작시에 로그인 되었으면 내 정보, 로그인 안되었으면 로그인 페이지로 이동

        if (rq.isLogout()) {
            return "redirect:/usr/member/login";
        }

        return "redirect:/usr/member/me";
    }

    @GetMapping("/usr/home/about")
    public String showAbout() {
        return "usr/home/about";
    }

    //세션 내용에 대한 디버그 나타내기(세션 내용 출력하기)
    @GetMapping("/usr/debugSession")
    @ResponseBody
    @PreAuthorize("hasAuthority('admin')")
    public String showDebugSession(HttpSession session) {
        StringBuilder sb = new StringBuilder("Session content:\n");

        Enumeration<String> attributeNames = session.getAttributeNames();   //열거 = 세션의 속성들

        //세션의 속성들에 대해 전부 출력.
        while (attributeNames.hasMoreElements()) {
            String attributeName = attributeNames.nextElement();
            Object attributeValue = session.getAttribute(attributeName);
            sb.append(String.format("%s: %s\n", attributeName, attributeValue));
        }

        return sb.toString().replaceAll("\n", "<br>");
    }


    @GetMapping("/usr/historyBackTest")
    @PreAuthorize("hasAuthority('admin')")
    public String showHistoryBackTest(HttpSession session)
    {
        return rq.historyBack("접근 금지 구역입니다.");
    }
}
