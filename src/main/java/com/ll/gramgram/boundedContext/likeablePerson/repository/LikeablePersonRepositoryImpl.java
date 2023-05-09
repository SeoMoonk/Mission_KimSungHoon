package com.ll.gramgram.boundedContext.likeablePerson.repository;

import com.ll.gramgram.boundedContext.instaMember.entity.InstaMember;
import com.ll.gramgram.boundedContext.likeablePerson.entity.LikeablePerson;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Optional;

import static com.ll.gramgram.boundedContext.likeablePerson.entity.QLikeablePerson.likeablePerson;

@RequiredArgsConstructor
public class LikeablePersonRepositoryImpl implements LikeablePersonRepositoryCustom {
    private final JPAQueryFactory jpaQueryFactory;

    @Override
    public Optional<LikeablePerson> findQslByFromInstaMemberIdAndToInstaMember_username(long fromInstaMemberId, String toInstaMemberUsername) {
        return Optional.ofNullable(
                jpaQueryFactory
                        .selectFrom(likeablePerson)
                        .where(
                                likeablePerson.fromInstaMember.id.eq(fromInstaMemberId)
                                        .and(
                                                likeablePerson.toInstaMember.username.eq(toInstaMemberUsername)
                                        )
                        )
                        .fetchOne()
        );
    }

    //호감목록에서, 내거면서, 타입코드가 00인것들
    //호감목록에서, 내거면서, 나를 좋아하는 사람의 gender 가 00인 것들
    //호감목록에서, 내거면서, 나를 좋아하는 사람의 gender 가 00이고, 타입코드가 00인 것들
    @Override
    public List<LikeablePerson> findQslByToInstaMemberIdAndFromInstaMember_gender(long toInstaMemberId, String gender) {
        return jpaQueryFactory
                .selectFrom(likeablePerson)
                .where(
                        likeablePerson.toInstaMember.id.eq(toInstaMemberId)
                                .and(
                                        likeablePerson.fromInstaMember.gender.eq(gender)
                                )
                )
                .fetch();
    }

    @Override
    public List<LikeablePerson> findQslByToInstaMemberIdAndAttractiveTypeCode(long toInstaMemberId, int attractiveTypeCode) {
        return jpaQueryFactory
                .selectFrom(likeablePerson)
                .where(
                        likeablePerson.toInstaMember.id.eq(toInstaMemberId)
                                .and(
                                        likeablePerson.attractiveTypeCode.eq(attractiveTypeCode)
                                )
                )
                .fetch();
    }
}