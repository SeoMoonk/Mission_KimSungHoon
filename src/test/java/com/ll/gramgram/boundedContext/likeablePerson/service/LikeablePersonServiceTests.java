package com.ll.gramgram.boundedContext.likeablePerson.service;


import com.ll.gramgram.TestUt;
import com.ll.gramgram.base.appConfig.AppConfig;
import com.ll.gramgram.boundedContext.instaMember.entity.InstaMember;
import com.ll.gramgram.boundedContext.likeablePerson.entity.LikeablePerson;
import com.ll.gramgram.boundedContext.likeablePerson.repository.LikeablePersonRepository;
import com.ll.gramgram.boundedContext.member.entity.Member;
import com.ll.gramgram.boundedContext.member.service.MemberService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.MethodName.class)
public class LikeablePersonServiceTests {

    @Autowired
    private MemberService memberService;

    @Autowired
    private LikeablePersonService likeablePersonService;

    @Autowired
    private LikeablePersonRepository likeablePersonRepository;

    @Test
    @DisplayName("yml 파일에서 호감 표시에 대한 수정 쿨타임 가져오기")
    void t006() throws Exception {

        System.out.println("likeablePersonModifyCoolDown : " + AppConfig.getLikeablePersonModifyCoolDown());

        assertThat(AppConfig.getLikeablePersonModifyCoolDown()).isGreaterThan(0);

    }

    @Test
    @DisplayName("LikeablePerson 엔티티가 생성될 때 제한 해제가 언제 되는지도 함께 저장되도록")
    void t007() throws Exception {

        //쿨타임
        LocalDateTime coolDown = AppConfig.genLikeablePersonModifyUnlockDate();

        Member memberUser3 = memberService.findByUsername("user3").orElseThrow();

        LikeablePerson likeablePersonToBts = likeablePersonService.like(memberUser3, "bts", 3).getData();

        //쿨타임을 받아오는 것이 호감표시보다 먼저 시작됬는데, 시간차이가 잘 나는가? (미세한 초 단위)
        assertThat(likeablePersonToBts.getModifyUnlockDate().isAfter(coolDown)).isTrue();

        //반대로
        LikeablePerson likeablePersonToBP = likeablePersonService.like(memberUser3, "BP", 1).getData(); //FIXME

        LocalDateTime coolDown2 = AppConfig.genLikeablePersonModifyUnlockDate();

        assertThat(likeablePersonToBP.getModifyUnlockDate().isAfter(coolDown2)).isFalse();

    }

}

