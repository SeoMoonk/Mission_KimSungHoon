package com.ll.gramgram.boundedContext.likeablePerson.service;

import com.ll.gramgram.DataNotFoundException;
import com.ll.gramgram.base.rsData.RsData;
import com.ll.gramgram.boundedContext.instaMember.entity.InstaMember;
import com.ll.gramgram.boundedContext.instaMember.service.InstaMemberService;
import com.ll.gramgram.boundedContext.likeablePerson.entity.LikeablePerson;
import com.ll.gramgram.boundedContext.likeablePerson.repository.LikeablePersonRepository;
import com.ll.gramgram.boundedContext.member.entity.Member;
import com.ll.gramgram.boundedContext.member.service.MemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LikeablePersonService {
    private final LikeablePersonRepository likeablePersonRepository;
    private final InstaMemberService instaMemberService;
    private final MemberService memberService;

    @Transactional
    public RsData<LikeablePerson> like(Member member, String username, int attractiveTypeCode) {
        if ( member.hasConnectedInstaMember() == false ) {
            return RsData.of("F-2", "먼저 본인의 인스타그램 아이디를 입력해야 합니다.");
        }

        if (member.getInstaMember().getUsername().equals(username)) {
            return RsData.of("F-1", "본인을 호감상대로 등록할 수 없습니다.");
        }

        InstaMember toInstaMember = instaMemberService.findByUsernameOrCreate(username).getData();

        LikeablePerson likeablePerson = LikeablePerson
                .builder()
                .fromInstaMember(member.getInstaMember()) // 호감을 표시하는 사람의 인스타 멤버
                .fromInstaMemberUsername(member.getInstaMember().getUsername()) // 중요하지 않음
                .toInstaMember(toInstaMember) // 호감을 받는 사람의 인스타 멤버
                .toInstaMemberUsername(toInstaMember.getUsername()) // 중요하지 않음
                .attractiveTypeCode(attractiveTypeCode) // 1=외모, 2=능력, 3=성격
                .build();

        likeablePersonRepository.save(likeablePerson); // 저장

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


    @Transactional      //DB 작업으로 수행될 수 있도록
    public void like_deleteById(Long id)
    {
        //올바른 id 값으로 객체를 얻어올 수 있도록 한 메서드를 호출하여 객체를 얻음.
        LikeablePerson deletePerson = getLikeablePersonById(id);

        InstaMember toInstaMember = deletePerson.getToInstaMember();

        //좋아했던 관계 삭제
        likeablePersonRepository.deleteById(deletePerson.getId());

        //내가 좋아했던 사람이 회원가입 되지 않았던 의문의 사람이라면 (gender가 U 라면) -> 인스타 멤버에서도 삭제.
        //단, 문제점 -> 의문의 사람을 이상하게도 나만 좋아하던게 아닐 수도 있다... 이 점은 나중에 추가하면 좋을듯...!
        if (toInstaMember.getGender().equals("U"))
        {
            //필요없어진 인스타멤버 객체 삭제
            instaMemberService.delete(toInstaMember);
        }
        else
        {
            //시스템 입출력보다는 예외처리로 만들어 주는게 좋지만.. 일단 생략
            System.out.println("삭제하려던 사용자는 우리의 회원입니다!");
        }
    }


    public Member getMemberByPrincipal_username(String principal_username)
    {
        //권한 검증을 위해 사용자의 이름을 가지고 Member 객체를 가져옴.
        Optional<Member> memberByUsername = memberService.getMemberByUsername(principal_username);

        if(memberByUsername.isPresent())
        {
            return memberByUsername.get();
        }
        else
        {
            //예외처리
            throw new DataNotFoundException("받아온 사용자 이름값으로는 데이터를 불러올 수 없습니다.");
        }
    }
}
