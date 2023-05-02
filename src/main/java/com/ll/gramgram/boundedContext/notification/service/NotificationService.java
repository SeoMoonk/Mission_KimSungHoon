package com.ll.gramgram.boundedContext.notification.service;

import com.ll.gramgram.boundedContext.instaMember.entity.InstaMember;
import com.ll.gramgram.boundedContext.likeablePerson.entity.LikeablePerson;
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

    //내가 호감표시를 받았거나, 나에 대한 호감사유가 변경된 경우, 알림페이지에서 확인이 가능하도록.
    // => 나에 대한 호감 표시가 생성거나 변경되면(like, modify), ** 호감표시를 받은 사람의 입장에서 ** 알림이 생성된다.

    //좋아요 생성에 따른 알림이 생성되었습니다.
    public void createNotification(LikeablePerson likeablePerson){

        //누가, 누구를, 좋아한다는 내용으로(Like or ModifyAttractiveType),
        //성별 바뀌지 않았음(null), 호감사유 변경이 아님(0), 새로운 성별 O ("M"), 새로운 호감 타입코드(TypeCode)
        // + 베이스 엔티티 (id, createDate, modifyDate)
        // + readDate (수신 날짜)
        Notification createNotification = Notification
                .builder()
                .id(likeablePerson.getId())                                   //호감 내용의 id
                .createDate(likeablePerson.getCreateDate())                   //호감 내용 생성 날짜
                .modifyDate(likeablePerson.getModifyDate())                   //호감 내용 수정 날짜
                .fromInstaMember(likeablePerson.getFromInstaMember())         //호감을 준 사람의 인스타 계정
                .toInstaMember(likeablePerson.getToInstaMember())             //호감을 받은 사람의 인스타 계정 (= 나)
                .typeCode("Like")                                           //알림 등록 유형(Like or Modify)
                .oldGender(null)                                              //FIXME 성별 수정인 경우, 이전 성별
                .oldAttractiveTypeCode(0)                                     //이전엔 나를 뭐때문에 좋아했는데?
                .newGender(likeablePerson.getToInstaMember().getGender())     //FIXME 성별 수정인 경우, 바뀐 성별
                .newAttractiveTypeCode(likeablePerson.getAttractiveTypeCode())//이전과 다르게 나를 뭐로 좋아하게 됐는데?
                .readDate(LocalDateTime.now())                                //FIXME 생성시에 null 이고, 읽었을 때 now
                .build();

        notificationRepository.save(createNotification);
    }

    //FIXME
    //다시 생각해 봤을때, beforeLikeablePerson과 afterLikeablePerson은 같은 내용을 가리키고 있기 때문에
    //다른 내용을 불러올 것 같지가 않음.
    public void modifyNotification(LikeablePerson beforeLikeablePerson, LikeablePerson afterLikeablePerson){

        //호감사유가 변경 되더라도, 기존 알림의 변경이 아닌 새로운 알림의 시작이기 떄문에 새로 생성해 주는 것이 맞다고 생각함.
        Notification modifyNotification = Notification
                .builder()
                .id(afterLikeablePerson.getId())                                   //호감 내용의 id
                .createDate(afterLikeablePerson.getCreateDate())                   //호감 내용 생성 날짜
                .modifyDate(afterLikeablePerson.getModifyDate())                   //호감 내용 수정 날짜
                .fromInstaMember(afterLikeablePerson.getFromInstaMember())         //호감을 준 사람의 인스타 계정
                .toInstaMember(afterLikeablePerson.getToInstaMember())             //호감을 받은 사람의 인스타 계정 (= 나)
                .typeCode("ModifyAttractiveType")                                  //알림 등록 유형(Like or Modify)
                .oldGender(beforeLikeablePerson.getToInstaMember().getGender())    //FIXME 성별 수정인 경우, 이전 성별
                .oldAttractiveTypeCode(beforeLikeablePerson.getAttractiveTypeCode())//이전엔 나를 뭐때문에 좋아했는데?
                .newGender(afterLikeablePerson.getToInstaMember().getGender())     //FIXME 성별 수정인 경우, 바뀐 성별
                .newAttractiveTypeCode(afterLikeablePerson.getAttractiveTypeCode())//이전과 다르게 나를 뭐로 좋아하게 됐는데?
                .readDate(LocalDateTime.now())                                     //FIXME 생성시에 null 이고, 읽었을 때 now
                .build();

        notificationRepository.save(modifyNotification);

    }

}