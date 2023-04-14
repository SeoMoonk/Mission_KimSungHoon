package com.ll.gramgram.boundedContext.likeablePerson.service;


import com.ll.gramgram.boundedContext.instaMember.entity.InstaMember;
import com.ll.gramgram.boundedContext.likeablePerson.entity.LikeablePerson;
import com.ll.gramgram.boundedContext.likeablePerson.repository.LikeablePersonRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
@ActiveProfiles("test")
public class LikeablePersonServiceTests {

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
            System.out.println("V4 : 기존 호감사유 : %S".formatted(oldLikeablePerson.getAttractiveTypeDisplayName()));
        }
    }


}

