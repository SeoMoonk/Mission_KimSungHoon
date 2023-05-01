package com.ll.gramgram.boundedContext.likeablePerson.service;

import com.ll.gramgram.DataNotFoundException;
import com.ll.gramgram.base.appConfig.AppConfig;
import com.ll.gramgram.base.event.EventAfterLike;
import com.ll.gramgram.base.event.EventAfterModifyAttractiveType;
import com.ll.gramgram.base.event.EventBeforeCancelLike;
import com.ll.gramgram.base.rsData.RsData;
import com.ll.gramgram.boundedContext.instaMember.entity.InstaMember;
import com.ll.gramgram.boundedContext.instaMember.service.InstaMemberService;
import com.ll.gramgram.boundedContext.likeablePerson.entity.LikeablePerson;
import com.ll.gramgram.boundedContext.likeablePerson.repository.LikeablePersonRepository;
import com.ll.gramgram.boundedContext.member.entity.Member;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LikeablePersonService {

    private final LikeablePersonRepository likeablePersonRepository;
    private final InstaMemberService instaMemberService;
    private final ApplicationEventPublisher publisher;

    @Transactional
    public RsData<LikeablePerson> like(Member actor, String username, int attractiveTypeCode)
    {
        RsData canILikeRsData = canILike(actor, username, attractiveTypeCode);

        if(canILikeRsData.isFail() || canILikeRsData.getResultCode().equals("S-6")) return canILikeRsData;

        InstaMember fromInstaMember = actor.getInstaMember();
        InstaMember toInstaMember = instaMemberService.findByUsernameOrCreate(username).getData();

        LikeablePerson likeablePerson = LikeablePerson
                .builder()
                .fromInstaMember(fromInstaMember) // 호감을 표시하는 사람의 인스타 멤버
                .fromInstaMemberUsername(actor.getInstaMember().getUsername())
                .toInstaMember(toInstaMember) // 호감을 받는 사람의 인스타 멤버
                .toInstaMemberUsername(toInstaMember.getUsername())
                .attractiveTypeCode(attractiveTypeCode)
                .modifyUnlockDate(AppConfig.genLikeablePersonModifyUnlockDate())    //제한 해제시간 설정
                .build();

        likeablePersonRepository.save(likeablePerson); // 저장

        //(Actor 입장)내가 좋아하는 사람이 생겼다.
        fromInstaMember.addFromLikeablePerson(likeablePerson);

        //(상대 입장)나를 좋아해주는 사람이 생겼다.
        toInstaMember.addToLikeablePerson(likeablePerson);

        publisher.publishEvent(new EventAfterLike(this, likeablePerson));

        canILikeRsData.setData(likeablePerson);

        //정상처리
        return canILikeRsData;
    }

    //내가 그 사람을 좋아할 수 있는 조건이 될까?
    public RsData canILike(Member actor, String username, int attractiveTypeCode)
    {
        InstaMember fromInstaMember = actor.getInstaMember();
        InstaMember toInstaMember = instaMemberService.findByUsernameOrCreate(username).getData();

        // F-2 본인 Instagram id 등록 후 사용 가능
        if (!actor.hasConnectedInstaMember())
            return RsData.of("F-2", "먼저 본인의 인스타그램 아이디를 입력해야 합니다.");

        //F-3 본인 등록 금지
        if (actor.getInstaMember().getUsername().equals(username))
            return RsData.of("F-3", "본인을 호감상대로 등록할 수 없습니다.");

        //FIX -> 리스트를 순회하는 것에서 "a가 b 를 좋아하는 관계" 자체가 이미 있다면 으로 변경
        LikeablePerson likeablePerson = getLikeablePerson(fromInstaMember, toInstaMember);

        if(likeablePerson != null)
        {
            if(likeablePerson.getAttractiveTypeCode() == attractiveTypeCode)
            {
                return RsData.of("F-4", "%s : 이미 호감으로 등록된 회원입니다.".formatted(username));
            }
            else
            {
                int prev_attractiveTypeCode = likeablePerson.getAttractiveTypeCode();

                //여기서도 수정할 수 있는지 체크하고, 막혀야 함.
                RsData canModify_rsData = canModifyLike(actor, likeablePerson);

                if(canModify_rsData.isFail()) {
                    return canModify_rsData;
                }

                //수정처리 메서드 별도로 호출하여 수행
                modifyAttractive(likeablePerson, attractiveTypeCode);

                return RsData.of("S-6", "(수정) %s에 대한 호감사유를 %s에서 %s으로 변경합니다."
                        .formatted(username, whatAttractiveTypeByCode(prev_attractiveTypeCode),
                                whatAttractiveTypeByCode(attractiveTypeCode)));
            }
        }

        long size =  likeablePersonRepository.countByFromInstaMemberId(fromInstaMember.getId());

        // F-5 : 호감 등록은 10명까지 가능 (count(size) 케이스는 modify 케이스보다 아래에 와야함.)
        if(size >= AppConfig.getLikeablePersonFromMax())
            return RsData.of("F-5", "호감 등록은 10명 까지만 가능합니다.");

        return RsData.of("S-1", "입력하신 인스타유저(%s)가 호감상대로 등록되었습니다.".formatted(username), likeablePerson);
    }

    public List<LikeablePerson> findByFromInstaMemberId(Long fromInstaMemberId) {
        return likeablePersonRepository.findByFromInstaMemberId(fromInstaMemberId);
    }

    //올바른 id 값으로 객체를 얻어올 수 있도록 하는 메서드
    public LikeablePerson getLikeablePersonById(Long id)
    {
        Optional<LikeablePerson> getLikeable = this.likeablePersonRepository.findById(id);

        if(getLikeable.isPresent())
        {
            return getLikeable.get();
        }
        else
        {
            throw new DataNotFoundException("해당 ID를 가진 사용자를 찾을 수 없습니다.");
        }
    }

    @Transactional
    public RsData cancel(LikeablePerson likeablePerson)
    {
//        likeablePerson.getFromInstaMember().removeFromLikeablePerson(likeablePerson);
//
//        likeablePerson.getToInstaMember().removeToLikeablePerson(likeablePerson);


        publisher.publishEvent(new EventBeforeCancelLike(this, likeablePerson));

        likeablePersonRepository.delete(likeablePerson);

        String likeCanceledUsername = likeablePerson.getToInstaMember().getUsername();
        return RsData.of("S-1", "%s님에 대한 호감을 취소하였습니다.".formatted(likeCanceledUsername));
    }

    public RsData canCancel(Member actor, LikeablePerson likeablePerson)
    {

        if (likeablePerson == null)
            return RsData.of("F-1", "이미 삭제되었습니다.");

        // 수행자의 인스타계정 번호
        long actorInstaMemberId = actor.getInstaMember().getId();

        // 삭제 대상의 작성자(호감표시한 사람)의 인스타계정 번호
        long fromInstaMemberId = likeablePerson.getFromInstaMember().getId();

        if (actorInstaMemberId != fromInstaMemberId)
            return RsData.of("F-2", "삭제 권한이 없습니다.");

        return RsData.of("S-1", "삭제가능합니다.");
    }

    //호감코드에 따라 어떤 내용인지를 얻어오는 메서드.
    public String whatAttractiveTypeByCode(int attractiveTypeCode)
    {
        return switch (attractiveTypeCode) {
            case 1 -> "외모";
            case 2 -> "성격";
            default -> "능력";
        };
    }

    public LikeablePerson getLikeablePerson(InstaMember fromInstaMember, InstaMember toInstaMember) {
        return likeablePersonRepository.findByFromInstaMemberIdAndToInstaMemberId
                (fromInstaMember.getId(), toInstaMember.getId()).orElse(null);
    }

    @Transactional
    public void modifyAttractive(LikeablePerson likeablePerson, int attractiveTypeCode) {
       likeablePerson.updateAttractionTypeCode(attractiveTypeCode);
    }

    @Transactional
    public RsData<LikeablePerson> modifyAttractive(Member actor, Long id, int attractiveTypeCode) {

        LikeablePerson likeablePersonById = getLikeablePersonById(id);

        return modifyAttractive(actor, likeablePersonById, attractiveTypeCode);
    }

    @Transactional
    public RsData<LikeablePerson> modifyAttractive(Member actor, LikeablePerson likeablePerson, int attractiveTypeCode)
    {
        RsData canModifyRsData = canModifyLike(actor, likeablePerson);

        if (canModifyRsData.isFail()) {
            return canModifyRsData;
        }

        String oldAttractiveTypeDisplayName = likeablePerson.getAttractiveTypeDisplayName();
        String username = likeablePerson.getToInstaMember().getUsername();

        modifyAttractionTypeCode(likeablePerson, attractiveTypeCode);

        String newAttractiveTypeDisplayName = likeablePerson.getAttractiveTypeDisplayName();

        return RsData.of("S-3", "%s님에 대한 호감사유를 %s에서 %s(으)로 변경합니다.".formatted(username, oldAttractiveTypeDisplayName, newAttractiveTypeDisplayName), likeablePerson);
    }

    public RsData<LikeablePerson> modifyAttractive(Member actor, String username, int attractiveTypeCode) {
        // 액터가 생성한 `좋아요` 들 가져오기
        List<LikeablePerson> fromLikeablePeople = actor.getInstaMember().getFromLikeablePeople();

        LikeablePerson fromLikeablePerson = fromLikeablePeople
                .stream()
                .filter(e -> e.getToInstaMember().getUsername().equals(username))
                .findFirst()
                .orElse(null);

        if (fromLikeablePerson == null) {
            return RsData.of("F-7", "호감표시를 하지 않았습니다.");
        }

        return modifyAttractive(actor, fromLikeablePerson, attractiveTypeCode);
    }

    @Transactional
    public RsData<LikeablePerson> modifyLike(Member actor, Long id, int attractiveTypeCode) {
        LikeablePerson likeablePerson = getLikeablePersonById(id);
        RsData canModifyRsData = canModifyLike(actor, likeablePerson);

        if (canModifyRsData.isFail()) {
            return canModifyRsData;
        }

        modifyAttractionTypeCode(likeablePerson, attractiveTypeCode);

        return RsData.of("S-1", "호감사유를 수정하였습니다.");
    }

    private void modifyAttractionTypeCode(LikeablePerson likeablePerson, int attractiveTypeCode) {
        int oldAttractiveTypeCode = likeablePerson.getAttractiveTypeCode();
        RsData rsData = likeablePerson.updateAttractionTypeCode(attractiveTypeCode);

        if (rsData.isSuccess()) {
            publisher.publishEvent(new EventAfterModifyAttractiveType(this, likeablePerson, oldAttractiveTypeCode, attractiveTypeCode));
        }
    }

    public RsData canModifyLike(Member actor, LikeablePerson likeablePerson) {

        if (!actor.hasConnectedInstaMember()) {
            return RsData.of("F-1", "먼저 본인의 인스타그램 아이디를 입력해주세요.");
        }

        InstaMember fromInstaMember = actor.getInstaMember();

        if (!Objects.equals(likeablePerson.getFromInstaMember().getId(), fromInstaMember.getId())) {
            return RsData.of("F-2", "해당 호감표시를 취소할 권한이 없습니다.");
        }

        return RsData.of("S-1", "호감표시취소가 가능합니다.");
    }
}
