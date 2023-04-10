package com.ll.gramgram.boundedContext.likeablePerson.service;

import com.ll.gramgram.DataNotFoundException;
import com.ll.gramgram.base.rsData.RsData;
import com.ll.gramgram.boundedContext.instaMember.entity.InstaMember;
import com.ll.gramgram.boundedContext.instaMember.service.InstaMemberService;
import com.ll.gramgram.boundedContext.likeablePerson.entity.LikeablePerson;
import com.ll.gramgram.boundedContext.likeablePerson.repository.LikeablePersonRepository;
import com.ll.gramgram.boundedContext.member.entity.Member;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LikeablePersonService {
    private final LikeablePersonRepository likeablePersonRepository;
    private final InstaMemberService instaMemberService;

    @Transactional
    public RsData<LikeablePerson> like(Member member, String username, int attractiveTypeCode) {

        //내가 좋아하는 사람들 리스트
        List<LikeablePerson> like_list = member.getInstaMember().getFromLikeablePeople();
        boolean isModifyDuplicate = false;

        if ( member.hasConnectedInstaMember() == false ) {
            return RsData.of("F-2", "먼저 본인의 인스타그램 아이디를 입력해야 합니다.");
        }

        if (member.getInstaMember().getUsername().equals(username)) {
            return RsData.of("F-1", "본인을 호감상대로 등록할 수 없습니다.");
        }

        // CASE4 : 중복으로 호감표시 금지
        for(int i=0; i<like_list.size(); i++)
        {
            LikeablePerson toInstaMemberByList = like_list.get(i);
            String toInstaMemberUsername = toInstaMemberByList.getToInstaMemberUsername();
            int list_attractiveTypeCode = toInstaMemberByList.getAttractiveTypeCode();

            if(toInstaMemberUsername.equals(username))
            {
                // CASE6 : 중복이지만 사유가 다를 경우에는 수정처리
                if(list_attractiveTypeCode != attractiveTypeCode){
                    //세터는 사용하지 않는것이 좋음. 그럼 HOW?
                    toInstaMemberByList.setAttractiveTypeCode(attractiveTypeCode);
                    System.out.println("호감 내용이 수정되었습니다.");
                    return RsData.of("S-6", "호감 내용이 수정되었습니다.");
                }

                //중복인데 사유도 다르지 않을 경우
                System.out.println("이미 호감으로 등록된 회원입니다.");
                return RsData.of("F-4", "%s : 이미 호감으로 등록된 회원입니다.".formatted(username));

            }
        }

        // CASE5 : 호감 등록은 10명까지 가능
        if(like_list.size() >= 10)
        {
            System.out.println("호감 등록은 10명까지만 가능합니다.");
            return RsData.of("F-5", "호감 등록은 10명 까지만 가능합니다.");
        }

        InstaMember fromInstaMember = member.getInstaMember();
        InstaMember toInstaMember = instaMemberService.findByUsernameOrCreate(username).getData();

        LikeablePerson likeablePerson = LikeablePerson
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

        return RsData.of("S-1", "입력하신 인스타유저(%s)를 호감상대로 등록되었습니다.".formatted(username), likeablePerson);
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


    public RsData canActorDelete(Member actor, LikeablePerson likeablePerson) {
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
}
