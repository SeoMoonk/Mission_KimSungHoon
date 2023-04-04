package com.ll.gramgram.boundedContext.member.service;

import com.ll.gramgram.base.rsData.RsData;
import com.ll.gramgram.boundedContext.instaMember.entity.InstaMember;
import com.ll.gramgram.boundedContext.member.entity.Member;
import com.ll.gramgram.boundedContext.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true) // 아래 메서드들이 전부 readonly 라는 것을 명시, 나중을 위해
public class MemberService {
    private final PasswordEncoder passwordEncoder;
    private final MemberRepository memberRepository;

    public Optional<Member> findByUsername(String username) {
        return memberRepository.findByUsername(username);
    }

    //메서드 오버로딩 -> 일반 회원가입의 경우 username, password 만 입력받고,
    //소셜 로그인은 providerTypeCode 까지 입력된다.
    //(일반 가입은 providerTypeCode 가 "GRAMGRAM" default 값으로 join 실행됨.)
    @Transactional // SELECT 이외의 쿼리에 대한 가능성이 아주 조금이라도 있으면 붙인다.
    // 일반 회원가입(소셜 로그인을 통한 회원가입이 아님)
    public RsData<Member> join(String username, String password) {

        // "GRAMGRAM" 해당 회원이 일반회원가입으로 인해 생성되었다는걸 나타내기 위해서
        return join("GRAMGRAM", username, password);

    }

    // 내부 처리함수, 일반회원가입, 소셜로그인을 통한 회원가입(최초 로그인 시 한번만 발생)에서 이 함수를 사용함
    private RsData<Member> join(String providerTypeCode, String username, String password) {

        if (findByUsername(username).isPresent()) {
            return RsData.of("F-1", "해당 아이디(%s)는 이미 사용중입니다.".formatted(username));
        }

        // 소셜 로그인을 통한 회원가입에서는 비번이 없다. (비밀번호에 내용이 있는 경우에만 암호화가 실행됨.)
        if (StringUtils.hasText(password)) password = passwordEncoder.encode(password);

        //빌더 패턴을 사용해 member 객체 생성
        Member member = Member
                .builder()
                .providerTypeCode(providerTypeCode)
                .username(username)
                .password(password)
                .build();

        //MemberRepository 를 통해 member 객체 저장
        memberRepository.save(member);

        return RsData.of("S-1", "회원가입이 완료되었습니다.", member);
    }

    // 해당 회원에게 인스타 계정을 연결시킨다.
    // 1:1 관계
    @Transactional
    public void updateInstaMember(Member member, InstaMember instaMember) {
        member.setInstaMember(instaMember);
        memberRepository.save(member); // 여기서 실제로 UPDATE 쿼리 발생 (이미 저장된 member 에 instaMember 가 추가되는 것이므로.)
    }

    // 소셜 로그인(카카오, 구글, 네이버) 로그인이 될 때 마다 실행되는 함수
    @Transactional                      //소셜로그인에는 비밀번호가 없기때문에, 로그인 종류와 username 으로 로그인을 수행함.
    public RsData<Member> whenSocialLogin(String providerTypeCode, String username) {
        Optional<Member> opMember = findByUsername(username); // username 예시 : KAKAO__1312319038130912, NAVER__1230812300

        //findByUsername(소셜아이디) 를 통해 유효한 값이 들어왔고, 값이 존재한다면 -> 로그인 가능.
        if (opMember.isPresent()) return RsData.of("S-2", "로그인 되었습니다.", opMember.get());

        // 소셜 로그인를 통한 가입시 비번은 없다. -> 로그인 수행
        return join(providerTypeCode, username, ""); // 최초 로그인 시 딱 한번 실행
    }

    public Optional<Member> getMemberByUsername(String username)
    {
        return this.memberRepository.findByUsername(username);
    }

}
