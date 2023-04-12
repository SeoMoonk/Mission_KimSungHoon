package com.ll.gramgram.boundedContext.likeablePerson.service;

import com.ll.gramgram.DataNotFoundException;
import com.ll.gramgram.base.Appconfig.AppConfig;
import com.ll.gramgram.base.rsData.RsData;
import com.ll.gramgram.boundedContext.instaMember.entity.InstaMember;
import com.ll.gramgram.boundedContext.instaMember.service.InstaMemberService;
import com.ll.gramgram.boundedContext.likeablePerson.entity.LikeablePerson;
import com.ll.gramgram.boundedContext.likeablePerson.repository.LikeablePersonRepository;
import com.ll.gramgram.boundedContext.member.entity.Member;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LikeablePersonService {
    private final LikeablePersonRepository likeablePersonRepository;
    private final InstaMemberService instaMemberService;

    //FIXME 좋지 않은 방법
    int prev_attractiveTypeCode = -1;

    @Transactional
    public RsData<LikeablePerson> like(Member member, String username, int attractiveTypeCode)
    {
        int canILike_code = canILike(member, username, attractiveTypeCode);
        LikeablePerson likeablePerson = null;   //FIXME

        switch(canILike_code)
        {
            case 1 :
                //@PreAuthorize("isAuthenticated()") ->
            case 2 :
                return RsData.of("F-2", "먼저 본인의 인스타그램 아이디를 입력해야 합니다.");
            case 3 :
                return RsData.of("F-3", "본인을 호감상대로 등록할 수 없습니다.");
            case 4 :
                return RsData.of("F-4", "%s : 이미 호감으로 등록된 회원입니다.".formatted(username));
            case 5 :
                return RsData.of("F-5", "호감 등록은 10명 까지만 가능합니다.");
            case 6 :
                return RsData.of("S-6", "(수정) %s에 대한 호감사유를 %s에서 %s으로 변경합니다."
                        .formatted(username, whatAttractiveTypeByCode(prev_attractiveTypeCode), whatAttractiveTypeByCode(attractiveTypeCode)));
                                                                        //FIXME 얻어올 방법 찾기
            case 0 :
                InstaMember fromInstaMember = member.getInstaMember();
                InstaMember toInstaMember = instaMemberService.findByUsernameOrCreate(username).getData();

                likeablePerson = LikeablePerson
                        .builder()
                        .fromInstaMember(fromInstaMember) // 호감을 표시하는 사람의 인스타 멤버
                        .fromInstaMemberUsername(member.getInstaMember().getUsername()) // 중요하지 않음
                        .toInstaMember(toInstaMember) // 호감을 받는 사람의 인스타 멤버
                        .toInstaMemberUsername(toInstaMember.getUsername()) // 중요하지 않음
                        .attractiveTypeCode(attractiveTypeCode)
                        .build();

                likeablePersonRepository.save(likeablePerson); // 저장

                //내가 좋아하는 사람이 생겼다.
                fromInstaMember.addFromLikeablePerson(likeablePerson);

                //나를 좋아해주는 사람이 생겼다.
                toInstaMember.addToLikeablePerson(likeablePerson);
        }
        //정상처리
        return RsData.of("S-1", "입력하신 인스타유저(%s)가 호감상대로 등록되었습니다.".formatted(username), likeablePerson);
    }

    //내가 그 사람을 좋아할 수 있는 조건이 될까?
    public int canILike(Member member, String username, int attractiveTypeCode)
    {
        List<LikeablePerson> like_list = member.getInstaMember().getFromLikeablePeople();

        // F-2 본인 Instagram id 등록 후 사용 가능
        if (!member.hasConnectedInstaMember()) return 2;

        //F-3 본인 등록 금지
        if (member.getInstaMember().getUsername().equals(username)) return 3;

        // F-4,6 : 중복으로 호감표시 금지


        for (LikeablePerson toInstaMemberByList : like_list)
        {
            String toInstaMemberUsername = toInstaMemberByList.getToInstaMemberUsername();
            int list_attractiveTypeCode = toInstaMemberByList.getAttractiveTypeCode();

            if (toInstaMemberUsername.equals(username))
            {
                // F-6 중복이지만 이유가 변경된 경유
                if (list_attractiveTypeCode != attractiveTypeCode)
                {
                    toInstaMemberByList.modify_attractiveTypeCode(attractiveTypeCode);
                    prev_attractiveTypeCode = list_attractiveTypeCode; //FIXME
                    return 6;
                }
                // F-4 중복인데 사유도 다르지 않을 경우
                return 4;
            }
        }



        // F-5 : 호감 등록은 10명까지 가능 (count(size) 케이스는 modify 케이스보다 아래에 와야함.)
        if(like_list.size() >= AppConfig.getLikeablePersonFromMax())
            return 5;

        return 0;
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
    public RsData delete(LikeablePerson likeablePerson)
    {
        likeablePersonRepository.delete(likeablePerson);

        String likeCanceledUsername = likeablePerson.getToInstaMember().getUsername();
        return RsData.of("S-1", "%s님에 대한 호감을 취소하였습니다.".formatted(likeCanceledUsername));
    }


    public RsData canActorDelete(Member actor, LikeablePerson likeablePerson)
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
}
