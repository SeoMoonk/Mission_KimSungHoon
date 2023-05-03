package com.ll.gramgram.boundedContext.notification.entity;

import com.ll.gramgram.base.baseEntity.BaseEntity;
import com.ll.gramgram.boundedContext.instaMember.entity.InstaMember;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

import java.time.Duration;
import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor
@SuperBuilder
@ToString(callSuper = true)
public class Notification extends BaseEntity {

    private LocalDateTime readDate;

    @ManyToOne
    @ToString.Exclude
    private InstaMember toInstaMember; // 메세지 받는 사람(호감 받는 사람)

    @ManyToOne
    @ToString.Exclude
    private InstaMember fromInstaMember; // 메세지를 발생시킨 행위를 한 사람(호감표시한 사람)

    private String typeCode; // 호감표시=Like, 호감사유변경=ModifyAttractiveType

    private String oldGender; // 해당사항 없으면 null

    private int oldAttractiveTypeCode; // 해당사항 없으면 0

    private String newGender; // 해당사항 없으면 null

    private int newAttractiveTypeCode; // 해당사항 없으면 0

    public String getAttractiveTypeDisplayName(int AttractiveTypeCode) {
        return switch (AttractiveTypeCode) {
            case 1 -> "외모";
            case 2 -> "성격";
            default -> "능력";
        };
    }

    public String getDateForDisplay(LocalDateTime dateForDisplay) {

        long month = dateForDisplay.getMonthValue();
        long day = dateForDisplay.getDayOfMonth();
        long hours = dateForDisplay.getHour();
        long minutes = dateForDisplay.getMinute();

        return "%02d월 %02d일 %02d시 %02d분".formatted(month, day, hours, minutes);
    }

    public String timePassed() {

        //등록 시간과 현재 시간이 얼마나 차이나는지  => 1분 미만일 경우 약 1분 전
        LocalDateTime nowDate = LocalDateTime.now();
        LocalDateTime notifCreateDate = this.getCreateDate();

        Duration duration = Duration.between(notifCreateDate, nowDate);

        long day = duration.toDays();
        long hours = duration.toHours();
        long minutes = duration.toMinutes();

        if(minutes < 1)
        {
            return "약 1분 전";
        }
        else if(minutes <= 59)
        {
            return "%02d 분 전".formatted(minutes);
        }
        else if(hours >= 1 && hours < 24)
        {
            return "%02d 시간 전".formatted(hours);
        }
        else if(day >= 1 && day <= 29)
        {
            return "%d 일 전".formatted(day);
        }
        else if(day > 30)
        {
            return "약 %02d 달 전".formatted(day/30);
        }

        return "%02d일 %02d시간 %02d 분 전".formatted(day, hours, minutes);
    }

    public void updateReadDate() {
        this.readDate = LocalDateTime.now();
    }

}