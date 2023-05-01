package com.ll.gramgram.boundedContext.notification.service;

import com.ll.gramgram.boundedContext.instaMember.entity.InstaMember;
import com.ll.gramgram.boundedContext.notification.entity.Notification;
import com.ll.gramgram.boundedContext.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;

    public List<Notification> findByToInstaMember(InstaMember toInstaMember) {
        return notificationRepository.findByToInstaMember(toInstaMember);
    }


    //누가, 누구를, 좋아한다는 내용으로(Like or ModifyAttractiveType),
    //성별 바뀌지 않았음(null), 호감사유 변경이 아님(null), 새로운 성별 O ("M"), 새로운 호감 타입코드(TypeCode)
    //알림이 생성되었습니다.
    public void createNotification(InstaMember fromInstaMember, InstaMember toInstaMember, String typeCode,
                                   String oldGender, int oldAttractiveTypeCode,
                                   String newGender, int newAttractiveTypeCode) {


        Notification newNotification = Notification.builder()
                .fromInstaMember(fromInstaMember)
                .toInstaMember(toInstaMember)
                .typeCode(typeCode)
                .oldGender(oldGender)
                .oldAttractiveTypeCode(oldAttractiveTypeCode)
                .newGender(newGender)
                .newAttractiveTypeCode(newAttractiveTypeCode)
                .id(1L)
                .createDate(LocalDateTime.now())
                .modifyDate(LocalDateTime.now())
                .build();

        notificationRepository.save(newNotification);

    }

}