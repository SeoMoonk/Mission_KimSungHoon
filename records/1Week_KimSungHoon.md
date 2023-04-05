# 1Week_KimSungHoon.md

## Title: [1Week] 김성훈

### 미션 요구사항 분석 & 체크리스트

#### 필수과제_배경
- 현재 호감목록 기능까지 구현되어 있다.
- 호감목록 페이지에서는 여태까지 본인이 호감을 표시한 상대방의 목록을 볼 수 있다.
- 현재 삭제버튼까지 구현되어 있다.

<br/>

#### 필수과제_목표
- [x] 삭제 처리를 하기 전에, 해당 내용을 삭제할 권한이 나에게 있는지 체크해야 한다.
- [x] 호감 목록 페이지의 삭제 버튼을 누르면, 해당 항목이 삭제되어야 한다.
- [x] 삭제를 진행한 후, rq.redirectWithMsg 함수를 사용하여 호감목록 페이지로 돌아와야 한다.

<br/>

#### 선택과제_배경
- 현재 일반 로그인과 카카오 로그인까지 구현되어있다.
  - Spring OAuth2 클라이언트로 구현되어 있음.

<br/>

#### 선택과제_목표
- [ ] 카카오 로그인과 마찬가지로 구글 로그인도 수행되어야 한다.
  - 스프링 OAuth2 클라이언트로 구현되어야 한다.
  - 구글 로그인으로 가입한 회원의 providerTypeCode : "GOOGLE" 이어야 한다.

---


### 1주차 미션 요약

#### - LikeablePerson 객체의 삭제 (필수)
#### - Google 소셜 로그인 구현 (선택)

---

### [접근 방법]

#### 필수 미션

1. Thymeleaf로 인해 삭제 버튼을 클릭하게 되면, **id 값이 템플릿에서 넘어오게 된다.**

<br/>

2. 해당 id 값을 가지고 삭제할 LikeablePerson 객체를 얻어올 수 있는 
   **getLikeablePersonById 메서드**를 구현하였다.
   (Optional 형태로 받아와서, DataNotFoundException 까지 진행된다.)

<br/>

3. 삭제를 진행하기 전 **권한 검증**이 필요한데, 현재 Principal(접근 주체) 객체에서 얻을 수 있는 내용은
   Member 객체의 username 이기 때문에, 이 username 을 가지고 **Member 객체가
   InstaMember 객체와 1:1 관계를 가지고 있다는 점을 이용하여** LikeableService에서
   MemberService 를 import 하고, MemberService 에서 **getMemberByUsername** 메서드를 구현하여
   <u>이름을 가지고 Member 객체를 얻을 수 있도록 하였다.</u> 받아온 Member 객체에는 말했듯이 InstaMember
   객체와의 관계를 지니고 있기 때문에. .getInstaMember().getUsername() 이 두가지 메서드를 수행하면
   결국 Principal에 들어있던(로그인한 사용자가 가입해둔 Instagram 아이디) 를 얻을 수 있게 된다.

<br/>

4. 결과적으로 정리하자면 **Member 엔티티는 InstaMember 엔티티와 (1:1)관계를 가지고 있고, LikeablePerson 
   엔티티도 InstaMember 엔티티와 (N:1)관계를 가지고 있기 때문에**, InstaMember 선에서 두 내용의 비교가
   가능하다는 점을 이용하여. Principal(접근 주체) 가 삭제하려는 내용이 삭제하려는 내용의 호감 주체
   (FromInstaMember) 와 일치한지를 확인하는 것으로 권한 검증을 구현하였다. 권한이 있다면
    원래의 목표였던 삭제 처리로 이어지도록 하였고, 권한이 확인되지 않았다면 AccessDeniedException을 발생시키도록 
    유도하였다. 
 
<br/>

5. 권한 검증이 완료되었다면 삭제 처리를 해야하는데, 삭제하려는 객체(deletePeronById)를 얻어올 수도
    있지만 우리가 원하는 쿼리문은 id를 통한 DELETE 문이기 때문에 like_deleteById(long id) 라는
    메서드를 LikeableService에 구현하였다.

<br/>

6. like_deleteById 메서드의 내용은 id 값을 기반으로 LikeablePerson 객체를 가져와서,
   FromInstaMember 와 ToInstaMember 의 내용을 null 로 설정해 주었다. 그 이유는 이 속성들에게
   InstagramMember 엔티티에 대한 ManyToOne (외래키) 관계가 이루어져 있기 때문에, 이러한 연결고리를
   끊어주어야 삭제가 이루어지기 때문이다. (더 좋은 방법은 분명 있을 것이다.) 그 후, JpaRepository 를
   상속받은 LikeablePersonRepository 의 deleteById 메서드를 통해 삭제를 진행하였다.
   추가적으로, 회원가입 되지 않았던 사람을 좋아했다면 그 내용을 삭제하는 내용도 구현하였는데. 이 내용은
   특이사항에 따로 서술하겠다.

<br/>

7. 삭제가 잘 마무리 되었다면, rq.redirectWithMsg 에 값으로 ("/likeablePerson/list",)
   "삭제가 완료되었습니다") 라는 값을 줌으로써 삭제 후에 redirect 요청으로 다시 목록으로
    이어질 수 있도록 하였고, "삭제가 완료되었습니다" 라는 메세지가 출력되도록 하였다.

<br/>

#### 선택 미션

1. 

---

### [특이 사항]

#### 필수 미션

- 삭제를 수행할 때, 내가 좋아했던 사람이 회원가입이 되지 않은 의문의 사람일 경우에,
해당 InstaMember 객체는 gender(성별)이 U(unknown) 으로 되어있다. 이러한 사람의 경우에
LikeableMember 테이블에서 삭제가 진행될 경우, InstaMember 테이블에서도 삭제되도록 기능을 추가하였다.
(단, 문제점 -> 의문의 사람을 나만 좋아하던 것이 아닐 수도 있다는 점까지 고려하지는 못했다.)

<br/>

#### 선택 미션
- 없음
