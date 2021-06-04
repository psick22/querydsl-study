package kirok.querydsl;


import com.querydsl.core.QueryResults;
import com.querydsl.core.Tuple;
import com.querydsl.jpa.impl.JPAQueryFactory;
import kirok.querydsl.entity.Member;
import kirok.querydsl.entity.Team;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import java.util.List;

import static kirok.querydsl.entity.QMember.member;
import static kirok.querydsl.entity.QTeam.team;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
public class QuerydslBasicTest {

    @Autowired
    EntityManager em;
    JPAQueryFactory queryFactory;

    @BeforeEach
    public void before() {
        queryFactory = new JPAQueryFactory(em);

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
    }

    @Test
    public void startJPQL() throws Exception {
        String qlString =
                "select m from Member m " +
                        "where m.username = :username";

        List<Member> resultList = em.createQuery(qlString, Member.class)
                                    .setParameter("username", "member1")
                                    .getResultList();

        assertThat(resultList.get(0).getUsername()).isEqualTo("member1");

    }

    @Test
    public void startQuerydsl() throws Exception {

        Member findMember = queryFactory.select(member)
                                        .from(member)
                                        .where(member.username.eq("member1"))
                                        .fetchOne();

        assertThat(findMember.getUsername()).isEqualTo("member1");
    }

    @Test
    public void 검색조건쿼리() throws Exception {

        Member findMember = queryFactory.selectFrom(member)
                                        .where(
                                                member.username.eq("member1"),
                                                member.age.between(10, 20)
                                                          .or(member.age.eq(15))
                                        )
                                        .fetchOne();

        assertThat(findMember.getUsername()).isEqualTo("member1");

    }

    @Test
    public void 결과조회쿼리() throws Exception {

        // List로 조회
        List<Member> fetch = queryFactory.selectFrom(member).fetch();

        // 단건 조회
        Member fetchOne = queryFactory.selectFrom(member).fetchOne();

        // 처음 한건
        Member fetchFirst = queryFactory.selectFrom(member).fetchFirst();

        // 페이징에서 사용
        QueryResults<Member> fetchResults = queryFactory.selectFrom(member).fetchResults();

        long total = fetchResults.getTotal();
        List<Member> results = fetchResults.getResults();

        long count = queryFactory.selectFrom(member).fetchCount();


    }

    @Test
    public void 정렬쿼리() throws Exception {
        em.persist(new Member(null, 100));
        em.persist(new Member("member5", 100));
        em.persist(new Member("member6", 100));

        List<Member> result = queryFactory
                .selectFrom(member)
                .where(member.age.eq(100))
                .orderBy(member.age.desc(), member.username.asc().nullsLast())
                .fetch();

        assertThat(result.get(0).getUsername()).isEqualTo("member5");
        assertThat(result.get(1).getUsername()).isEqualTo("member6");
        assertThat(result.get(2).getUsername()).isNull();


    }


    @Test
    public void 페이징쿼리() throws Exception {
        List<Member> result = queryFactory
                .selectFrom(member)
                .orderBy(member.username.desc())
                .offset(1)
                .limit(2)
                .fetch();

        assertThat(result.size()).isEqualTo(2);

    }

    @Test
    public void 페이징쿼리2() throws Exception {
        QueryResults<Member> results = queryFactory
                .selectFrom(member)
                .orderBy(member.username.desc())
                .offset(1)
                .limit(2)
                .fetchResults();

        assertThat(results.getTotal()).isEqualTo(4);
        assertThat(results.getLimit()).isEqualTo(2);
        assertThat(results.getOffset()).isEqualTo(1);
        assertThat(results.getResults().size()).isEqualTo(2);
    }

    @Test
    public void 집합쿼리() throws Exception {

        List<Tuple> result = queryFactory
                .select(
                        member.count(),
                        member.age.sum(),
                        member.age.avg(),
                        member.age.max(),
                        member.age.min()
                )
                .from(member)
                .fetch();

        Tuple tuple = result.get(0);

        assertThat(tuple.get(member.count())).isEqualTo(4);
        assertThat(tuple.get(member.age.sum())).isEqualTo(100);
        assertThat(tuple.get(member.age.avg())).isEqualTo(25);
        assertThat(tuple.get(member.age.max())).isEqualTo(40);
        assertThat(tuple.get(member.age.min())).isEqualTo(10);

    }

    /**
     * 팀의 이름과 각 팀의 평균 연령을 구하라
     */
    @Test
    public void group_by_쿼리() throws Exception {
        List<Tuple> result = queryFactory
                .select(team.name, member.age.avg())
                .from(member)
                .join(member.team, team)
                .groupBy(team.name)
                .fetch();

        assertThat(result.get(0).get(team.name)).isEqualTo("teamA");
        assertThat(result.get(0).get(member.age.avg())).isEqualTo(15);
        assertThat(result.get(1).get(team.name)).isEqualTo("teamB");
        assertThat(result.get(1).get(member.age.avg())).isEqualTo(35);
    }

    /**
     * 팀 A에 소속된 모든 회원
     */
    @Test
    public void 조인쿼리() throws Exception {

        List<Member> result = queryFactory
                .selectFrom(member)
                .join(team).on(team.name.eq("teamA"))
//                .where(team.name.eq("teamA"))
                .fetch();

        assertThat(result).extracting("username").containsExactly("member1", "member2");

    }

    /**
     * 회원의 이름이 팀이름과 같은 회원 조회
     */
    @Test
    public void 쎄타조인쿼리() throws Exception {

        em.persist(new Member("teamA"));
        em.persist(new Member("teamB"));
        em.persist(new Member("teamC ㅠ"));

        List<Member> result = queryFactory
                .select(member)
                .from(member, team)
                .where(member.username.eq(team.name))
                .fetch();

        assertThat(result).extracting("username").containsExactly("teamA", "teamB");
    }


    /**
     * 회원과 팀을 조인하면서, 팀이름이 teamA인 팀만 조인, 회원은 모두 조회´´´´
     * JPQL: select m, t from Member m left join m.team t on t.name = 'teamA'
     */
    @Test
    public void 조인ON절_필터링() throws Exception {


        List<Tuple> result = queryFactory
                .select(member, team)
                .from(member)
                .join(member.team, team)
                .on(team.name.eq("teamA"))
                .fetch();

        for (Tuple tuple : result) {
            System.out.println("tuple = " + tuple);
        }

    }

    /**
     * 회원의 이름이 팀이름과 같은 대상을 외부 조인
     */
    @Test
    public void 연관관계없는외부조인() throws Exception {

        em.persist(new Member("teamA"));
        em.persist(new Member("teamB"));
        em.persist(new Member("teamC"));

        List<Tuple> result = queryFactory
                .select(member, team)
                .from(member)
                .leftJoin(team).on(member.username.eq(team.name))
                .fetch();


        for (Tuple tuple : result) {
            System.out.println("tuple = " + tuple);
        }
    }


}
