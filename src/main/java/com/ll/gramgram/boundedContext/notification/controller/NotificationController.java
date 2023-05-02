package com.ll.gramgram.boundedContext.notification.controller;

import com.ll.gramgram.base.rq.Rq;
import com.ll.gramgram.boundedContext.member.entity.Member;
import com.ll.gramgram.boundedContext.notification.entity.Notification;
import com.ll.gramgram.boundedContext.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.Collections;
import java.util.List;

@Controller
@RequestMapping("/usr/notification")
@RequiredArgsConstructor
public class NotificationController {

    private final Rq rq;
    private final NotificationService notificationService;

    @GetMapping("/list")
    @PreAuthorize("isAuthenticated()")
    public String showList(Model model) {

        if (!rq.getMember().hasConnectedInstaMember()) {
            return rq.redirectWithMsg("/usr/instaMember/connect", "먼저 본인의 인스타그램 아이디를 입력해주세요.");
        }

        Member member = rq.getMember();

        //서비스에서 현재 접속한 유저의 인스타 계정을 통해 알릴만한 내용이 있는지 찾아서 model 로 넘겨줌.
        List<Notification> notifications = notificationService.findByToInstaMember(rq.getMember().getInstaMember());

        //매핑된 list URL 이 호출 되었을 때, 나에 대한 알림들의 읽은 시각을 현재 시각으로 업데이트
        notificationService.updateReadDate(member);

        //최신 알림이 상단에 오도록 역순으로 전송
        Collections.reverse(notifications);

        model.addAttribute("notifications", notifications);

        return "usr/notification/list";
    }
}
