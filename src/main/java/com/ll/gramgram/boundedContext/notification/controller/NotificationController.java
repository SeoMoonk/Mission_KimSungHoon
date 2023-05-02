package com.ll.gramgram.boundedContext.notification.controller;

import com.ll.gramgram.base.rq.Rq;
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

        //서비스에서 현재 접속한 유저의 인스타 계정을 통해 알릴만한 내용이 있는지 찾아서 model 로 넘겨줌.
        List<Notification> notifications = notificationService.findByToInstaMember(rq.getMember().getInstaMember());

        //최신 알림이 상단에 오도록 역순으로 전송
        Collections.reverse(notifications);

        model.addAttribute("notifications", notifications);

        //매핑된 list url이 호출 되었을 때, 알림 목록을 가져오고, 호출 되었을 때 읽은 시각을 처리할 수 있도록.

        return "usr/notification/list";
    }
}
