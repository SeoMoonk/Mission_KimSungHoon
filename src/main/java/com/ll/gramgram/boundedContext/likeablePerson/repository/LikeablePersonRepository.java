package com.ll.gramgram.boundedContext.likeablePerson.repository;

import com.ll.gramgram.boundedContext.likeablePerson.entity.LikeablePerson;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LikeablePersonRepository extends JpaRepository<LikeablePerson, Long>, LikeablePersonRepositoryCustom {
    List<LikeablePerson> findByFromInstaMemberId(Long fromInstaMemberId);

    List<LikeablePerson> findByToInstaMember_username(String username);

    LikeablePerson findByFromInstaMemberIdAndToInstaMember_username(long fromInstaMemberId, String username);

    Optional<LikeablePerson> findByFromInstaMember_usernameAndToInstaMember_username(String fromInstaMemberUsername, String toInstaMemberUsername);

    //List<LikeablePerson> findByFromInstaMemberGender(String gender);

    //호감목록에서, 내거면서, 타입코드가 00인것들
    //호감목록에서, 내거면서, 나를 좋아하는 사람의 gender 가 00인 것들
    //호감목록에서, 내거면서, 나를 좋아하는 사람의 gender 가 00이고, 타입코드가 00인 것들


}