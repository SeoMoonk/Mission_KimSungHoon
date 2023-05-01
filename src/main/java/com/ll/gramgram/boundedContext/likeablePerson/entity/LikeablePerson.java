package com.ll.gramgram.boundedContext.likeablePerson.entity;

import com.ll.gramgram.base.appConfig.AppConfig;
import com.ll.gramgram.base.rsData.RsData;
import com.ll.gramgram.boundedContext.instaMember.entity.InstaMember;
import com.ll.gramgram.standard.util.Ut;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Duration;
import java.time.LocalDateTime;

import static jakarta.persistence.GenerationType.IDENTITY;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
@ToString
@Entity
@Getter
public class LikeablePerson {

    @Id
    @GeneratedValue(strategy = IDENTITY)
    private Long id;

    @CreatedDate
    private LocalDateTime createDate;

    @LastModifiedDate
    private LocalDateTime modifyDate;

    @ManyToOne
    @ToString.Exclude
    private InstaMember fromInstaMember; // 호감을 표시한 사람(인스타 멤버)

    private String fromInstaMemberUsername; // 혹시 몰라서 기록

    @ManyToOne
    @ToString.Exclude
    private InstaMember toInstaMember; // 호감을 받은 사람(인스타 멤버)

    private String toInstaMemberUsername; // 혹시 몰라서 기록

    private int attractiveTypeCode; // 매력포인트(1=외모, 2=성격, 3=능력)

    private LocalDateTime modifyUnlockDate;   //제한 해제 시간

    public RsData updateAttractionTypeCode(int attractiveTypeCode) {
        if (this.attractiveTypeCode == attractiveTypeCode) {
            return RsData.of("F-1", "이미 설정되었습니다.");
        }

        this.attractiveTypeCode = attractiveTypeCode;
        this.modifyUnlockDate = AppConfig.genLikeablePersonModifyUnlockDate();

        return RsData.of("S-1", "성공");
    }

    public String getAttractiveTypeDisplayName() {
        return switch (attractiveTypeCode) {
            case 1 -> "외모";
            case 2 -> "성격";
            default -> "능력";
        };
    }

    public String getJdenticon() {
        return Ut.hash.sha256(fromInstaMember.getId() + "_likes_" + toInstaMember.getId());
    }


    public void modify_attractiveTypeCode(int attractiveTypeCode) {
        this.attractiveTypeCode = attractiveTypeCode;
        this.modifyDate = LocalDateTime.now();
    }

    public String getAttractiveTypeDisplayNameWithIcon() {
        return switch (attractiveTypeCode) {
            case 1 -> "<i class=\"fa-solid fa-person-rays\"></i>";
            case 2 -> "<i class=\"fa-regular fa-face-smile\"></i>";
            default -> "<i class=\"fa-solid fa-people-roof\"></i>";
        } + "&nbsp;" + getAttractiveTypeDisplayName();
    }

    public boolean isModifyUnlocked() {
        return modifyUnlockDate.isBefore(LocalDateTime.now());
    }

    //FIXME => 시간 남으면 좀 더 섬세하게
    public String getModifyUnlockDateRemainStrHuman() {

        LocalDateTime nowDate = LocalDateTime.now();
        LocalDateTime modifyUnlockDate = this.getModifyUnlockDate();

        Duration duration = Duration.between(nowDate, modifyUnlockDate);
        long hours = duration.toHours();
        long minutes = duration.toMinutes() % 60;

        return "%02d 시간 %02d 분 후".formatted(hours, minutes);
    }

}
