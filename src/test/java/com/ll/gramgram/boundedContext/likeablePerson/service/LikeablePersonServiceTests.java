package com.ll.gramgram.boundedContext.likeablePerson.service;


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
    @DisplayName("테스트1")
    void t001() throws Exception {

        // 좋아요 리스트 중에서 2번 항목을 가져온다.
        LikeablePerson likeablePersonById2 = likeablePersonService.getLikeablePersonById(2L);

        // 2번 좋아요 내역에서 호감을 표시한 회원의 InstaMember 을 가져온다.
        //그 회원의 username 은 insta_user3 이다.
        InstaMember instaMemberUser3 = likeablePersonById2.getFromInstaMember();
        assertThat(instaMemberUser3.getUsername()).isEqualTo("insta_user3");

        // 그 회원이 지금껏 좋아하고있는 사람들의 명단이다.
        // from(호감을 표시한 사람) to(호감을 받은 사람)
        List<LikeablePerson> fromLikeablePeople =instaMemberUser3.getFromLikeablePeople();

        for(LikeablePerson likeablePerson : fromLikeablePeople) {
            assertThat(instaMemberUser3.getUsername()).isEqualTo(likeablePerson.getFromInstaMember().getUsername());
        }
    }

    @Test
    @DisplayName("테스트 2")
    void t002() throws Exception {

        LikeablePerson likeablePersonId2 = likeablePersonService.getLikeablePersonById(2L);

        InstaMember instaMemberUser3 = likeablePersonId2.getFromInstaMember();

        String usernameToLike = "insta_user4";

        //V1
        LikeablePerson likeablePersonIndex0 = instaMemberUser3.getFromLikeablePeople().get(0);
        LikeablePerson likeablePersonIndex1 = instaMemberUser3.getFromLikeablePeople().get(1);

        if (usernameToLike.equals(likeablePersonIndex0.getToInstaMember().getUsername()))
            System.out.println("V1 : 중복으로 호감을 표시할 수 없습니다.");

        if (usernameToLike.equals(likeablePersonIndex1.getToInstaMemberUsername()))
            System.out.println("V1 : 중복으로 호감을 표시할 수 없습니다.");

        //V2
        for (LikeablePerson fromLikeablePerson : instaMemberUser3.getFromLikeablePeople()) {
            String toInstaMemberUsername = fromLikeablePerson.getToInstaMemberUsername();

            if (usernameToLike.equals(toInstaMemberUsername)) {
                System.out.println("V2 : 중복으로 호감을 표시할 수 없습니다.");
                break;
            }
        }

        //V3
        long count = instaMemberUser3
                .getFromLikeablePeople()
                .stream()
                .filter(lp -> lp.getToInstaMember().getUsername().equals(usernameToLike))
                .count();

        if (count > 0)
            System.out.println("V3 : 중복으로 호감을 표시할 수 없습니다.");

        //V4
        LikeablePerson oldLikeablePerson = instaMemberUser3
                .getFromLikeablePeople()
                .stream()
                .filter(lp -> lp.getToInstaMember().getUsername().equals(usernameToLike))
                .findFirst()
                .orElse(null);

        if (oldLikeablePerson != null) {
            System.out.println("V4 : 중복으로 호감을 표시할 수 없습니다.");
            System.out.printf("V4 : 기존 호감사유 : %s%n", oldLikeablePerson.getAttractiveTypeDisplayName());
        }
    }

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

        //FIXME : 3번 회원이 BTS 에게 3번 코드로 호감을 표시 => RsData.getData() 시에 NULL 발생.
        LikeablePerson likeablePersonToBts = likeablePersonService.like(memberUser3, "bts", 3).getData();

        //쿨타임을 받아오는 것이 호감표시보다 먼저 시작됬는데, 시간차이가 잘 나는가? (미세한 초 단위)
        assertThat(likeablePersonToBts.getModifyUnlockDate().isAfter(coolDown)).isTrue();

        //반대로
        LikeablePerson likeablePersonToBP = likeablePersonService.like(memberUser3, "BP", 1).getData(); //FIXME

        LocalDateTime coolDown2 = AppConfig.genLikeablePersonModifyUnlockDate();

        assertThat(likeablePersonToBP.getModifyUnlockDate().isAfter(coolDown2)).isFalse();

    }


}

