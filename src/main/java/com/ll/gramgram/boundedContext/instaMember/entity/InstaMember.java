package com.ll.gramgram.boundedContext.instaMember.entity;

import com.ll.gramgram.boundedContext.likeablePerson.entity.LikeablePerson;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.AbstractAuditable_;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.List;

import static jakarta.persistence.GenerationType.IDENTITY;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
@ToString
@Entity
@Getter
public class InstaMember {

    @Id
    @GeneratedValue(strategy = IDENTITY)
    private Long id;

    @CreatedDate
    private LocalDateTime createDate;

    @LastModifiedDate
    private LocalDateTime modifyDate;

    @Column(unique = true)
    private String username;

    @Setter
    private String gender;

//    //내가 좋아하는 사람들을 모아둠. (상대가 사라지면, 내가 좋아했던 내용도 사라짐.)
//    @OneToMany(mappedBy = "toInstaMember", cascade = CascadeType.REMOVE)
//    private List<LikeablePerson> toLikeList;
//
//    //나를 좋아하는 사람들을 모아둠. (내가 사라지면, 나를 좋아했던 사람들에게서도 사라짐.)
//    @OneToMany(mappedBy = "fromInstaMember")
//    private List<LikeablePerson> fromLikeList;


}
