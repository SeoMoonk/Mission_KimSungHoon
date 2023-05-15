package com.ll.gramgram.boundedContext.likeablePerson.service;

import com.ll.gramgram.base.appConfig.AppConfig;
import com.ll.gramgram.base.event.EventAfterLike;
import com.ll.gramgram.base.event.EventAfterModifyAttractiveType;
import com.ll.gramgram.base.event.EventBeforeCancelLike;
import com.ll.gramgram.base.rsData.RsData;
import com.ll.gramgram.boundedContext.instaMember.entity.InstaMember;
import com.ll.gramgram.boundedContext.instaMember.service.InstaMemberService;
import com.ll.gramgram.boundedContext.likeablePerson.entity.LikeablePerson;
import com.ll.gramgram.boundedContext.likeablePerson.repository.LikeablePersonRepository;
import com.ll.gramgram.boundedContext.member.entity.Member;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LikeablePersonService {
    private final LikeablePersonRepository likeablePersonRepository;
    private final InstaMemberService instaMemberService;
    private final ApplicationEventPublisher publisher;

    @Transactional
    public RsData<LikeablePerson> like(Member actor, String username, int attractiveTypeCode) {
        RsData canLikeRsData = canLike(actor, username, attractiveTypeCode);

        if (canLikeRsData.isFail()) return canLikeRsData;

        if (canLikeRsData.getResultCode().equals("S-2")) return modifyAttractive(actor, username, attractiveTypeCode);

        InstaMember fromInstaMember = actor.getInstaMember();
        InstaMember toInstaMember = instaMemberService.findByUsernameOrCreate(username).getData();

        LikeablePerson likeablePerson = LikeablePerson
                .builder()
                .fromInstaMember(fromInstaMember) // 호감을 표시하는 사람의 인스타 멤버
                .fromInstaMemberUsername(actor.getInstaMember().getUsername()) // 중요하지 않음
                .toInstaMember(toInstaMember) // 호감을 받는 사람의 인스타 멤버
                .toInstaMemberUsername(toInstaMember.getUsername()) // 중요하지 않음
                .attractiveTypeCode(attractiveTypeCode) // 1=외모, 2=능력, 3=성격
                .modifyUnlockDate(AppConfig.genLikeablePersonModifyUnlockDate())
                .build();

        likeablePersonRepository.save(likeablePerson); // 저장

        // 너가 좋아하는 호감표시 생겼어.
        fromInstaMember.addFromLikeablePerson(likeablePerson);

        // 너를 좋아하는 호감표시 생겼어.
        toInstaMember.addToLikeablePerson(likeablePerson);

        publisher.publishEvent(new EventAfterLike(this, likeablePerson));

        return RsData.of("S-1", "입력하신 인스타유저(%s)를 호감상대로 등록되었습니다.".formatted(username), likeablePerson);
    }

    public List<LikeablePerson> findByFromInstaMemberId(Long fromInstaMemberId) {
        return likeablePersonRepository.findByFromInstaMemberId(fromInstaMemberId);
    }

    public Optional<LikeablePerson> findById(Long id) {
        return likeablePersonRepository.findById(id);
    }

    @Transactional
    public RsData cancel(LikeablePerson likeablePerson) {
        publisher.publishEvent(new EventBeforeCancelLike(this, likeablePerson));

        // 너가 생성한 좋아요가 사라졌어.
        likeablePerson.getFromInstaMember().removeFromLikeablePerson(likeablePerson);

        // 너가 받은 좋아요가 사라졌어.
        likeablePerson.getToInstaMember().removeToLikeablePerson(likeablePerson);

        likeablePersonRepository.delete(likeablePerson);

        String likeCanceledUsername = likeablePerson.getToInstaMember().getUsername();
        return RsData.of("S-1", "%s님에 대한 호감을 취소하였습니다.".formatted(likeCanceledUsername));
    }

    public RsData canCancel(Member actor, LikeablePerson likeablePerson) {
        if (likeablePerson == null) return RsData.of("F-1", "이미 취소되었습니다.");

        // 수행자의 인스타계정 번호
        long actorInstaMemberId = actor.getInstaMember().getId();
        // 삭제 대상의 작성자(호감표시한 사람)의 인스타계정 번호
        long fromInstaMemberId = likeablePerson.getFromInstaMember().getId();

        if (actorInstaMemberId != fromInstaMemberId)
            return RsData.of("F-2", "취소할 권한이 없습니다.");

        if (!likeablePerson.isModifyUnlocked())
            return RsData.of("F-3", "아직 취소할 수 없습니다. %s에는 가능합니다.".formatted(likeablePerson.getModifyUnlockDateRemainStrHuman()));

        return RsData.of("S-1", "취소가 가능합니다.");
    }

    private RsData canLike(Member actor, String username, int attractiveTypeCode) {
        if (!actor.hasConnectedInstaMember()) {
            return RsData.of("F-1", "먼저 본인의 인스타그램 아이디를 입력해주세요.");
        }

        InstaMember fromInstaMember = actor.getInstaMember();

        if (fromInstaMember.getUsername().equals(username)) {
            return RsData.of("F-2", "본인을 호감상대로 등록할 수 없습니다.");
        }

        // 액터가 생성한 `좋아요` 들 가져오기
        List<LikeablePerson> fromLikeablePeople = fromInstaMember.getFromLikeablePeople();

        // 그 중에서 좋아하는 상대가 username 인 녀석이 혹시 있는지 체크
        LikeablePerson fromLikeablePerson = fromLikeablePeople
                .stream()
                .filter(e -> e.getToInstaMember().getUsername().equals(username))
                .findFirst()
                .orElse(null);

        if (fromLikeablePerson != null && fromLikeablePerson.getAttractiveTypeCode() == attractiveTypeCode) {
            return RsData.of("F-3", "이미 %s님에 대해서 호감표시를 했습니다.".formatted(username));
        }

        long likeablePersonFromMax = AppConfig.getLikeablePersonFromMax();

        if (fromLikeablePerson != null) {
            return RsData.of("S-2", "%s님에 대해서 호감표시가 가능합니다.".formatted(username));
        }

        if (fromLikeablePeople.size() >= likeablePersonFromMax) {
            return RsData.of("F-4", "최대 %d명에 대해서만 호감표시가 가능합니다.".formatted(likeablePersonFromMax));
        }

        return RsData.of("S-1", "%s님에 대해서 호감표시가 가능합니다.".formatted(username));
    }

    public Optional<LikeablePerson> findByFromInstaMember_usernameAndToInstaMember_username(String fromInstaMemberUsername, String toInstaMemberUsername) {
        return likeablePersonRepository.findByFromInstaMember_usernameAndToInstaMember_username(fromInstaMemberUsername, toInstaMemberUsername);
    }

    @Transactional
    public RsData<LikeablePerson> modifyAttractive(Member actor, Long id, int attractiveTypeCode) {
        Optional<LikeablePerson> likeablePersonOptional = findById(id);

        if (likeablePersonOptional.isEmpty()) {
            return RsData.of("F-1", "존재하지 않는 호감표시입니다.");
        }

        LikeablePerson likeablePerson = likeablePersonOptional.get();

        return modifyAttractive(actor, likeablePerson, attractiveTypeCode);
    }

    @Transactional
    public RsData<LikeablePerson> modifyAttractive(Member actor, LikeablePerson likeablePerson, int attractiveTypeCode) {
        RsData canModifyRsData = canModify(actor, likeablePerson);

        if (canModifyRsData.isFail()) {
            return canModifyRsData;
        }

        String oldAttractiveTypeDisplayName = likeablePerson.getAttractiveTypeDisplayName();
        String username = likeablePerson.getToInstaMember().getUsername();

        modifyAttractionTypeCode(likeablePerson, attractiveTypeCode);

        String newAttractiveTypeDisplayName = likeablePerson.getAttractiveTypeDisplayName();

        return RsData.of("S-3", "%s님에 대한 호감사유를 %s에서 %s(으)로 변경합니다.".formatted(username, oldAttractiveTypeDisplayName, newAttractiveTypeDisplayName), likeablePerson);
    }

    @Transactional
    public RsData<LikeablePerson> modifyAttractive(Member actor, String username, int attractiveTypeCode) {
        // 액터가 생성한 `좋아요` 들 가져오기
        List<LikeablePerson> fromLikeablePeople = actor.getInstaMember().getFromLikeablePeople();

        LikeablePerson fromLikeablePerson = fromLikeablePeople
                .stream()
                .filter(e -> e.getToInstaMember().getUsername().equals(username))
                .findFirst()
                .orElse(null);

        if (fromLikeablePerson == null) {
            return RsData.of("F-7", "호감표시를 하지 않았습니다.");
        }

        return modifyAttractive(actor, fromLikeablePerson, attractiveTypeCode);
    }

    private void modifyAttractionTypeCode(LikeablePerson likeablePerson, int attractiveTypeCode) {
        int oldAttractiveTypeCode = likeablePerson.getAttractiveTypeCode();
        RsData rsData = likeablePerson.updateAttractionTypeCode(attractiveTypeCode);

        if (rsData.isSuccess()) {
            publisher.publishEvent(new EventAfterModifyAttractiveType(this, likeablePerson, oldAttractiveTypeCode, attractiveTypeCode));
        }
    }

    public RsData canModify(Member actor, LikeablePerson likeablePerson) {
        if (!actor.hasConnectedInstaMember()) {
            return RsData.of("F-1", "먼저 본인의 인스타그램 아이디를 입력해주세요.");
        }

        InstaMember fromInstaMember = actor.getInstaMember();

        if (!Objects.equals(likeablePerson.getFromInstaMember().getId(), fromInstaMember.getId())) {
            return RsData.of("F-2", "해당 호감표시에 대해서 사유변경을 수행할 권한이 없습니다.");
        }

        if (!likeablePerson.isModifyUnlocked())
            return RsData.of("F-3", "아직 호감사유변경을 할 수 없습니다. %s에는 가능합니다.".formatted(likeablePerson.getModifyUnlockDateRemainStrHuman()));


        return RsData.of("S-1", "호감사유변경이 가능합니다.");
    }

//    public List<LikeablePerson> filteringByGender(List<LikeablePerson> likeablePeople, String gender) {
//
//        List<LikeablePerson> filteredListByGender = new ArrayList<>();
//
//        for (LikeablePerson likeablePerson : likeablePeople)
//        {
//            if (likeablePerson.getFromInstaMember().getGender().equals(gender))
//            {
//                filteredListByGender.add(likeablePerson);
//            }
//        }
//
//        return filteredListByGender;
//    }

//    public List<LikeablePerson> filteringByGenderQuery(long toInstaMemberId, String gender) {
//
//        List<LikeablePerson> filteredListByGender = likeablePersonRepository.findQslByToInstaMemberIdAndFromInstaMember_gender(toInstaMemberId,gender);
//
//        return filteredListByGender;
//    }

//    public List<LikeablePerson> filteringByTypeCode(List<LikeablePerson> likeablePeople, String typeCode) {
//
//        List<LikeablePerson> filteredListByTypeCode = new ArrayList<>();
//        int attractiveTypeCode = Integer.parseInt(typeCode);
//
//        for(LikeablePerson likeablePerson : likeablePeople)
//        {
//            if(attractiveTypeCode == likeablePerson.getAttractiveTypeCode())
//            {
//                filteredListByTypeCode.add(likeablePerson);
//            }
//        }
//
//        return filteredListByTypeCode;
//    }

//    public List<LikeablePerson> filteringByTypeCodeQuery(long toInstaMemberId, String attractiveTypeCode){
//
//        int integerTypeCode = Integer.parseInt(attractiveTypeCode);
//
//        List<LikeablePerson> filteredListByTypeCode =
//                likeablePersonRepository.findQslByToInstaMemberIdAndAttractiveTypeCode(toInstaMemberId, integerTypeCode);
//
//        //이렇게 해도 됨.
//        //return likeablePersonRepository.findQslByToInstaMemberIdAndAttractiveTypeCode(toInstaMemberId, integerTypeCode);
//
//        return filteredListByTypeCode;
//    }

//    public List<LikeablePerson> filteringByGenderAndTypeCode(List<LikeablePerson> filteredListByGender,
//                                                             List<LikeablePerson> filteredListByTypeCode) {
//        List<LikeablePerson> filteredListByGenderAndTypeCode = new ArrayList<>();
//
//        for(LikeablePerson likeablePerson1 : filteredListByGender)
//        {
//            for(LikeablePerson likeablePerson2 : filteredListByTypeCode)
//            {
//                if(likeablePerson1.getId() == likeablePerson2.getId())
//                {
//                    filteredListByGenderAndTypeCode.add(likeablePerson1);
//                    break;
//                }
//            }
//        }
//
//        return filteredListByGenderAndTypeCode;
//    }

//    public List<LikeablePerson> filteringByGenderAndTypeCode(List<LikeablePerson> likeablePeople, String gender, String typeCode){
//
//        List<LikeablePerson> filteredListByGenderAndTypeCode = new ArrayList<>();
//
//        int attractiveTypeCode = Integer.parseInt(typeCode);
//
//        for(LikeablePerson likeablePerson : likeablePeople)
//        {
//            if(likeablePerson.getFromInstaMember().getGender().equals(gender) &&
//                    likeablePerson.getAttractiveTypeCode() == attractiveTypeCode) {
//
//                filteredListByGenderAndTypeCode.add(likeablePerson);
//
//            }
//        }
//
//        return filteredListByGenderAndTypeCode;
//    }

    public boolean hasGenderFilter(String gender){

        if(gender == null || gender.equals("")) {
            return false;
        }
        else if(gender.equals("W") || gender.equals("M")){
            return true;
        }

        return false;
    }

    public boolean hasTypeCodeFilter(String typeCode) {

        if(typeCode == null || typeCode.equals("")){
            return false;
        }
        else if(typeCode.equals("1") || typeCode.equals("2") || typeCode.equals("3")) {
            return true;
        }

        return false;
    }

    public Stream<LikeablePerson> getStreamByFiltering(InstaMember instaMember, String gender, String attractiveTypeCode) {

        List<LikeablePerson> likeablePeople = instaMember.getToLikeablePeople();
        Stream<LikeablePerson> likeablePeopleStream = likeablePeople.stream();

        boolean hasGenderFilter = hasGenderFilter(gender);
        boolean hasTypeCodeFilter = hasTypeCodeFilter(attractiveTypeCode);

        int integerAttractiveTypeCode;

        if(hasTypeCodeFilter) {
            integerAttractiveTypeCode = Integer.parseInt(attractiveTypeCode);
        } else {
            integerAttractiveTypeCode = -1;
        }

        if(!hasGenderFilter && !hasTypeCodeFilter)
        {
            //추가 작업이 필요 없음. => 걸러내기 위해 제일 상단 배치.
        }
        else if(hasGenderFilter && hasTypeCodeFilter)
        {
            Stream<LikeablePerson> genderAndtypeCodeStream = likeablePeopleStream
                    .filter(likeablePerson ->
                            likeablePerson.getFromInstaMember().getGender().equals(gender)
                                    && likeablePerson.getAttractiveTypeCode() == integerAttractiveTypeCode);

            likeablePeopleStream = genderAndtypeCodeStream;
        }
        else if(hasGenderFilter)
        {
            //V1 (단순 조회) => 나에 대한 호감에서 조건에 맞는 것들만 리스트에 추가하는 방법
            //List<LikeablePerson> filteredListByGender = likeablePersonService.filteringByGender(likeablePeople, gender);

            //V2 (쿼리 사용) => JPA Query를 이용해 조건에 맞는 것들만 찾아오도록 하는 방법
            //List<LikeablePerson> filteredListByGenderQsl = likeablePersonService.filteringByGenderQuery(instaMember.getId(), gender);

            //V3 (Stream 의 filter 메서드)
            Stream<LikeablePerson> genderStream = likeablePeopleStream
                    .filter(likeablePerson ->
                            likeablePerson.getFromInstaMember().getGender().equals(gender));

            likeablePeopleStream = genderStream;

        }
        else if(hasTypeCodeFilter)
        {
            Stream<LikeablePerson> typeCodeStream = likeablePeopleStream
                    .filter(likeablePerson ->
                            likeablePerson.getAttractiveTypeCode() == integerAttractiveTypeCode);

            likeablePeopleStream = typeCodeStream;
        }

        return likeablePeopleStream;
    }

    public Comparator<LikeablePerson> getComparator(String sortCode){

        int integerSortCode = Integer.parseInt(sortCode);

        Comparator<LikeablePerson> comparator = null;

        switch(integerSortCode) {
            case 1: //최신순
                comparator = Comparator.comparing(LikeablePerson::getCreateDate).reversed();
                break;
            case 2: //오래된순
                comparator = Comparator.comparing(LikeablePerson::getCreateDate);
                break;
            case 3: //가장 인기가 많은 사람들의 호감표시를 우선적으로 표시
                comparator = Comparator.comparing(likeablePerson ->
                        likeablePerson.getFromInstaMember().getLikes(), Comparator.reverseOrder());
                break;
            case 4: //가장 인기가 적은 사람들의 호감표시를 우선적으로 표시
                comparator = Comparator.comparing(likeablePerson ->
                        likeablePerson.getFromInstaMember().getLikes());
                break;
            case 5: //성별순(여성먼저 표시 후 남성 표시, 2순위 조건은 최신순)
                comparator = Comparator
                        .<LikeablePerson, String> comparing(likeablePerson ->
                                likeablePerson.getFromInstaMember().getGender().equals("W") ? "0" : "1")
                        .thenComparing(likeablePerson -> likeablePerson.getFromInstaMember().getGender().equals("M") ? "0" : "1")
                        .thenComparing(Comparator.comparing(LikeablePerson::getCreateDate).reversed());
                break;
            case 6: //호감사유순(1.외모부터 -> 2.성격 -> 3.능력순서로, 2순위 조건은 최신순)
                comparator = Comparator.comparing(LikeablePerson::getAttractiveTypeCode).reversed()
                        .thenComparing(LikeablePerson::getCreateDate).reversed();
                break;
        }

        return comparator;
    }
}