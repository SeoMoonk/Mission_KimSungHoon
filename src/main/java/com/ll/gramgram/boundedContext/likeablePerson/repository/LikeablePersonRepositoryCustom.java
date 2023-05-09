package com.ll.gramgram.boundedContext.likeablePerson.repository;

import com.ll.gramgram.boundedContext.likeablePerson.entity.LikeablePerson;

import java.util.List;
import java.util.Optional;

public interface LikeablePersonRepositoryCustom {

    //호감목록에서, 내거면서, 타입코드가 00인것들
    //호감목록에서, 내거면서, 나를 좋아하는 사람의 gender 가 00인 것들
    //호감목록에서, 내거면서, 나를 좋아하는 사람의 gender 가 00이고, 타입코드가 00인 것들
    Optional<LikeablePerson> findQslByFromInstaMemberIdAndToInstaMember_username(long fromInstaMemberId, String toInstaMemberUsername);

    List<LikeablePerson> findQslByToInstaMemberIdAndFromInstaMember_gender(long toInstaMemberId, String gender);

    List<LikeablePerson> findQslByToInstaMemberIdAndAttractiveTypeCode(long toInstaMemberId, int attractiveTypeCode);


}