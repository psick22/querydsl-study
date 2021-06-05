package kirok.querydsl.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import javax.persistence.EntityManager;
import kirok.querydsl.dto.MemberSearchDto;
import kirok.querydsl.entity.Member;
import kirok.querydsl.entity.Team;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class MemberJpaRepositoryTest {

    @Autowired
    EntityManager em;

    @Autowired
    MemberJpaRepository memberJpaRepository;


    @Test
    public void basicTest() {

        Member member = new Member("member1", 10);
        memberJpaRepository.save(member);

        Member findMember = memberJpaRepository.findById(member.getId()).get();

        assertThat(member).isEqualTo(findMember);

        List<Member> result = memberJpaRepository.findAll();

        assertThat(result.size()).isEqualTo(1);
        assertThat(result.get(0).getUsername()).isEqualTo("member1");

        List<Member> findByUsername = memberJpaRepository.findByUsername("member1");

        assertThat(findByUsername).containsExactly(member);
    }

    @Test
    public void basicQuerydslTest() {
        Member member = new Member("member1", 10);
        memberJpaRepository.save(member);

        Member findMember = memberJpaRepository.findById(member.getId()).get();

        assertThat(member).isEqualTo(findMember);

        List<Member> result = memberJpaRepository.findAllV2();

        assertThat(result.size()).isEqualTo(1);
        assertThat(result.get(0).getUsername()).isEqualTo("member1");

        List<Member> findByUsername = memberJpaRepository.findByUsernameV2("member1");

        assertThat(findByUsername).containsExactly(member);


    }

    @Test
    public void searchTest() throws Exception {

        //given
        Team teamA = new Team("teamA");
        Team teamB = new Team("teamB");
        em.persist(teamA);
        em.persist(teamB);

        Member member1 = new Member("member1", 10, teamA);
        Member member2 = new Member("member2", 20, teamA);
        Member member3 = new Member("member3", 30, teamB);
        Member member4 = new Member("member4", 40, teamB);
        em.persist(member1);
        em.persist(member2);
        em.persist(member3);
        em.persist(member4);

        MemberSearchDto condition = new MemberSearchDto();

        condition.setAgeGoe(30);
        condition.setAgeLoe(40);
        condition.setTeamName("");

        //when
        List<Member> result = memberJpaRepository.searchMember(condition);

        for (Member memberTeamDto : result) {
            System.out.println("memberTeamDto = " + memberTeamDto);
        }

        System.out.println("result = " + result);
        //then
        assertThat(result).extracting("username").containsExactly("member3", "member4");
    }


}