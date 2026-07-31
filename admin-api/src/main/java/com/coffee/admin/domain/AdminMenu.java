package com.coffee.admin.domain;

/**
 * 어드민기능(여기서 어드민이랑 내부적으로 사용할, 다른팀에게 제공할)
 * 운영팀에서 다양한 조건의 검색을 요구하는 경우가 있는데 (동적검색 가령,작성자가 누구냐, 작성시간,다양한정렬)
 * 검색이 도메인이냐 하면(검색엔진을 만들지 않는 일반적인 경우) 아니라고 본다.
 * 이런걸 도메인으로 둔다면 더 중요한 도메인과 구분?하기 어렵게 될 수 있고 본질을 해칠 수 있음
 * 아무튼 어드민기능 때문에 서비스 쪽 수정이 일어날 일 없게 방지하는 기계적인 방식예를들어 find(a,b) 메서드가 있을 때 어드민쪽 기능편하게 find(a,b,Condition c) 이렇게넣다가 결국 findAdmin 이런 선택하는 사람들 많이 봄
 * 만약 어드민이 커져서 영향도가 심각해 졌을 때 해당 모듈을 별도 서버로 띄울 수도 있음
 * 또 장점이 협업중 팀원이 어드민 기능 수정을 했다해서 코드리뷰를 봤더니 코어모듈을 건들였다면 "왜 여기 부분 건들였어요?"라고 이야기 나오기 쉬움 (팀원 본인도 인식하기 쉬움)
 * READ.MD 에 해당 모듈은 느슨한 구조를 하고있다 명시하면 좋음
 */
public class AdminMenu {
    private Long id;
    private String name;
    private AdminPrice adminPrice;
    private String imageUrl;
    private String description;
    private AdminMenuDetail detail;
}
