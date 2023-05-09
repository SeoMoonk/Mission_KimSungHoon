package com.ll.gramgram.boundedContext.likeablePerson.repository;

import com.ll.gramgram.boundedContext.likeablePerson.entity.LikeablePerson;

import java.util.List;
import java.util.Optional;

public interface LikeablePersonRepositoryCustom {

    Optional<LikeablePerson> findQslByFromInstaMemberIdAndToInstaMember_username(long fromInstaMemberId, String toInstaMemberUsername);

    List<LikeablePerson> findQslByToInstaMemberIdAndFromInstaMember_gender(long toInstaMemberId, String gender);

    List<LikeablePerson> findQslByToInstaMemberIdAndAttractiveTypeCode(long toInstaMemberId, int attractiveTypeCode);


}