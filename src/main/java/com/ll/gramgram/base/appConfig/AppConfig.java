package com.ll.gramgram.base.appConfig;


import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDateTime;

@Configuration
public class AppConfig {

    @Getter
    private static long likeablePersonFromMax;

    @Getter
    private static long likeablePersonModifyCoolDown;

    @Value("${custom.likeablePerson.from.max}")
    public void setLikeablePersonFromMax(long likeablePersonFromMax) {
        AppConfig.likeablePersonFromMax = likeablePersonFromMax;
    }

    @Value("${custom.likeablePerson.modifyCoolDown}")
    public void setLikeablePersonModifyCoolDown(long likeablePersonModifyCoolDown) {
        AppConfig.likeablePersonModifyCoolDown = likeablePersonModifyCoolDown;
    }

    //현재시간 + 쿨타임(3시간) => 수정/삭제 제한 해제 시간 generate
    public static LocalDateTime genLikeablePersonModifyUnlockDate() {
        return LocalDateTime.now().plusSeconds(likeablePersonModifyCoolDown);
    }

}
