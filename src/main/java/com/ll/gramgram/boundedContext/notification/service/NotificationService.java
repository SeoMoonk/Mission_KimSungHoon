package com.ll.gramgram.boundedContext.notification.service;

import com.ll.gramgram.boundedContext.instaMember.entity.InstaMember;
import com.ll.gramgram.boundedContext.likeablePerson.entity.LikeablePerson;
import com.ll.gramgram.boundedContext.notification.entity.Notification;
import com.ll.gramgram.boundedContext.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;

    public List<Notification> findByToInstaMember(InstaMember toInstaMember) {
        return notificationRepository.findByToInstaMember(toInstaMember);
    }

    public void createNotification(LikeablePerson likeablePerson)
    {

        Notification createNotification = Notification
                .builder()
                .createDate(likeablePerson.getCreateDate())                   //호감 내용 생성 날짜 (base)
                .modifyDate(likeablePerson.getModifyDate())                   //호감 내용 수정 날짜 (base)
                .fromInstaMember(likeablePerson.getFromInstaMember())         //호감을 준 사람의 인스타 계정
                .toInstaMember(likeablePerson.getToInstaMember())             //호감을 받은 사람의 인스타 계정 (= 나)
                .typeCode("Like")                                             //알림 등록 유형(Like or Modify)
                .oldGender(null)                                              //FIXME 성별 수정인 경우, 이전 성별
                .oldAttractiveTypeCode(0)                                     //이전엔 나를 뭐때문에 좋아했는데?
                .newGender(null)                                              //FIXME 성별 수정인 경우, 바뀐 성별
                .newAttractiveTypeCode(likeablePerson.getAttractiveTypeCode())//이전과 다르게 나를 뭐로 좋아하게 됐는데?
                .readDate(null)                                               //FIXME 생성시에 null 이고, 읽었을 때 now
                .build();

        notificationRepository.save(createNotification);
    }


    public void modifyNotification(LikeablePerson likeablePerson, int oldAttractiveTypeCode)
    {

        Notification modifyNotification = Notification
                .builder()
                .createDate(likeablePerson.getCreateDate())                   //호감 내용 생성 날짜 (base)
                .modifyDate(likeablePerson.getModifyDate())                   //호감 내용 수정 날짜 (base)
                .fromInstaMember(likeablePerson.getFromInstaMember())         //호감을 준 사람의 인스타 계정
                .toInstaMember(likeablePerson.getToInstaMember())             //호감을 받은 사람의 인스타 계정 (= 나)
                .typeCode("ModifyAttractiveType")                             //알림 등록 유형(Like or Modify)
                .oldGender(null)                                              //FIXME 성별 수정인 경우, 이전 성별
                .oldAttractiveTypeCode(oldAttractiveTypeCode)                 //이전엔 나를 뭐때문에 좋아했는데?
                .newGender(null)                                              //FIXME 성별 수정인 경우, 바뀐 성별
                .newAttractiveTypeCode(likeablePerson.getAttractiveTypeCode())//이전과 다르게 나를 뭐로 좋아하게 됐는데?
                .readDate(null)                                //FIXME 생성시에 null 이고, 읽었을 때 now
                .build();

        //호감사유는 변경된 것 이라도, 기존 알림의 변경이 아닌 새로운 알림의 시작이기 떄문에 새로 생성해 주는 것이 맞다고 생각함.
        notificationRepository.save(modifyNotification);
    }

    @Transactional
    public void updateReadDate()
    {
        //readDate 가 null 인 것만 가져와서, 현재 시각으로 바꿔줌
        List<Notification> byReadDateIsNull = notificationRepository.findByReadDateIsNull();

        if(!byReadDateIsNull.isEmpty())
        {
            for(int i=0; i<byReadDateIsNull.size(); i++)
            {
                Notification notificationForUpdate = byReadDateIsNull.get(i);
                notificationForUpdate.updateReadDate();
            }
        }
    }

}