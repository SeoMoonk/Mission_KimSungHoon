package com.ll.gramgram.boundedContext.likeablePerson.controller;

import com.ll.gramgram.base.rq.Rq;
import com.ll.gramgram.base.rsData.RsData;
import com.ll.gramgram.boundedContext.instaMember.entity.InstaMember;
import com.ll.gramgram.boundedContext.likeablePerson.entity.LikeablePerson;
import com.ll.gramgram.boundedContext.likeablePerson.service.LikeablePersonService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Controller
@RequestMapping("/usr/likeablePerson")
@RequiredArgsConstructor
public class LikeablePersonController {
    private final Rq rq;
    private final LikeablePersonService likeablePersonService;

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/like")
    public String showLike() {
        return "usr/likeablePerson/like";
    }

    @AllArgsConstructor
    @Getter
    public static class LikeForm {
        @NotBlank
        @Size(min = 3, max = 30)
        private final String username;
        @NotNull
        @Min(1)
        @Max(3)
        private final int attractiveTypeCode;
    }

    @PreAuthorize("isAuthenticated()")
    @PostMapping("/like")
    public String like(@Valid LikeForm likeForm) {
        RsData<LikeablePerson> rsData = likeablePersonService.like(rq.getMember(), likeForm.getUsername(), likeForm.getAttractiveTypeCode());

        if (rsData.isFail()) {
            return rq.historyBack(rsData);
        }

        return rq.redirectWithMsg("/usr/likeablePerson/list", rsData);
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/list")
    public String showList(Model model) {
        InstaMember instaMember = rq.getMember().getInstaMember();

        // 인스타인증을 했는지 체크
        if (instaMember != null) {
            // 해당 인스타회원이 좋아하는 사람들 목록
            List<LikeablePerson> likeablePeople = instaMember.getFromLikeablePeople();
            model.addAttribute("likeablePeople", likeablePeople);
        }

        return "usr/likeablePerson/list";
    }

    @PreAuthorize("isAuthenticated()")
    @DeleteMapping("/{id}")
    public String cancel(@PathVariable Long id) {
        LikeablePerson likeablePerson = likeablePersonService.findById(id).orElse(null);

        RsData canDeleteRsData = likeablePersonService.canCancel(rq.getMember(), likeablePerson);

        if (canDeleteRsData.isFail()) return rq.historyBack(canDeleteRsData);

        RsData deleteRsData = likeablePersonService.cancel(likeablePerson);

        if (deleteRsData.isFail()) return rq.historyBack(deleteRsData);

        return rq.redirectWithMsg("/usr/likeablePerson/list", deleteRsData);
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/modify/{id}")
    public String showModify(@PathVariable Long id, Model model) {
        LikeablePerson likeablePerson = likeablePersonService.findById(id).orElseThrow();

        RsData canModifyRsData = likeablePersonService.canModify(rq.getMember(), likeablePerson);

        if (canModifyRsData.isFail()) return rq.historyBack(canModifyRsData);

        model.addAttribute("likeablePerson", likeablePerson);

        return "usr/likeablePerson/modify";
    }

    @AllArgsConstructor
    @Getter
    public static class ModifyForm {
        @NotNull
        @Min(1)
        @Max(3)
        private final int attractiveTypeCode;
    }

    @PreAuthorize("isAuthenticated()")
    @PostMapping("/modify/{id}")
    public String modify(@PathVariable Long id, @Valid ModifyForm modifyForm) {
        RsData<LikeablePerson> rsData = likeablePersonService.modifyAttractive(rq.getMember(), id, modifyForm.getAttractiveTypeCode());

        if (rsData.isFail()) {
            return rq.historyBack(rsData);
        }

        return rq.redirectWithMsg("/usr/likeablePerson/list", rsData);
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/toList")
    public String showToList(@RequestParam(name = "gender", required = false) String gender,
                             @RequestParam(name = "attractiveTypeCode", required = false) String attractiveTypeCode,
                             @RequestParam(name = "sortCode", required = true, defaultValue = "1") String sortCode,
                             Model model) {

        InstaMember instaMember = rq.getMember().getInstaMember();

        if(instaMember != null )
        {
            List<LikeablePerson> likeablePeople = instaMember.getToLikeablePeople();
            Stream<LikeablePerson> likeablePeopleStream = likeablePeople.stream();

            boolean hasGenderFilter = likeablePersonService.hasGenderFilter(gender);
            boolean hasTypeCodeFilter = likeablePersonService.hasTypeCodeFilter(attractiveTypeCode);
            int integerAttractiveTypeCode;

            int integerSortCode = Integer.parseInt(sortCode);

            if(hasTypeCodeFilter) {
                integerAttractiveTypeCode = Integer.parseInt(attractiveTypeCode);
            } else {
                integerAttractiveTypeCode = -1;
            }

            if(!hasGenderFilter && !hasTypeCodeFilter)
            {
                //추가 작업이 필요 없음.
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
                //V1 (단순 조회)
                //List<LikeablePerson> filteredListByGender = likeablePersonService.filteringByGender(likeablePeople, gender);

                //V2 (쿼리 사용)
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

            Stream<LikeablePerson> sortedStream;
            Comparator<LikeablePerson> comparator = null;

            switch(integerSortCode) {
                case 1: //최신순
                    comparator = Comparator.comparing(LikeablePerson::getCreateDate).reversed();
                    break;
                case 2: //오래된순
                    comparator = Comparator.comparing(LikeablePerson::getCreateDate);
                    break;
                case 3: //가장 인기가 많은 사람들의 호감표시를 우선적으로 표시 (instaMember.getLikes( );)
                    break;
                case 4: //가장 인기가 적은 사람들의 호감표시를 우선적으로 표시
                    break;
                case 5: //성별순(여성먼저 표시 후 남성 표시, 2순위 조건은 최신순)
                    break;
                case 6: //호감사유순(외모부터 -> 성격 -> 능력순서로, 2순위 조건은 최신순)
                    break;
            }

            sortedStream = likeablePeopleStream.sorted(comparator);

            model.addAttribute("likeablePeople", sortedStream.toList());
        }

        return "usr/likeablePerson/toList";
    }
}