package kirok.querydsl;

import static com.querydsl.core.types.ExpressionUtils.as;
import static com.querydsl.core.types.Projections.bean;
import static com.querydsl.core.types.Projections.constructor;
import static com.querydsl.core.types.Projections.fields;
import static com.querydsl.jpa.JPAExpressions.select;
import static kirok.querydsl.entity.QMember.member;
import static kirok.querydsl.entity.QTeam.team;
import static org.assertj.core.api.Assertions.assertThat;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.QueryResults;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.List;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceUnit;
import kirok.querydsl.dto.MemberDto;
import kirok.querydsl.dto.QMemberDto;
import kirok.querydsl.dto.UserDto;
import kirok.querydsl.entity.Member;
import kirok.querydsl.entity.QMember;
import kirok.querydsl.entity.Team;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Commit;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
public class QuerydslBasicTest {

    @Autowired
    EntityManager em;
    JPAQueryFactory queryFactory;
    @PersistenceUnit
    EntityManagerFactory emf;

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
        String qlString = "select m from Member m " + "where m.username = :username";

        List<Member> resultList =
            em.createQuery(qlString, Member.class).setParameter("username", "member1")
                .getResultList();

        assertThat(resultList.get(0).getUsername()).isEqualTo("member1");
    }

    @Test
    public void startQuerydsl() throws Exception {

        Member findMember =
            queryFactory.select(member).from(member).where(member.username.eq("member1"))
                .fetchOne();

        assertThat(findMember.getUsername()).isEqualTo("member1");
    }

    @Test
    public void ??????????????????() throws Exception {

        Member findMember =
            queryFactory
                .selectFrom(member)
                .where(member.username.eq("member1"),
                    member.age.between(10, 20).or(member.age.eq(15)))
                .fetchOne();

        assertThat(findMember.getUsername()).isEqualTo("member1");
    }

    @Test
    public void ??????????????????() throws Exception {

        // List??? ??????
        List<Member> fetch = queryFactory.selectFrom(member).fetch();

        // ?????? ??????
        Member fetchOne = queryFactory.selectFrom(member).fetchOne();

        // ?????? ??????
        Member fetchFirst = queryFactory.selectFrom(member).fetchFirst();

        // ??????????????? ??????
        QueryResults<Member> fetchResults = queryFactory.selectFrom(member).fetchResults();

        long total = fetchResults.getTotal();
        List<Member> results = fetchResults.getResults();

        long count = queryFactory.selectFrom(member).fetchCount();
    }

    @Test
    public void ????????????() throws Exception {
        em.persist(new Member(null, 100));
        em.persist(new Member("member5", 100));
        em.persist(new Member("member6", 100));

        List<Member> result =
            queryFactory
                .selectFrom(member)
                .where(member.age.eq(100))
                .orderBy(member.age.desc(), member.username.asc().nullsLast())
                .fetch();

        assertThat(result.get(0).getUsername()).isEqualTo("member5");
        assertThat(result.get(1).getUsername()).isEqualTo("member6");
        assertThat(result.get(2).getUsername()).isNull();
    }

    @Test
    public void ???????????????() throws Exception {
        List<Member> result =
            queryFactory.selectFrom(member).orderBy(member.username.desc()).offset(1).limit(2)
                .fetch();

        assertThat(result.size()).isEqualTo(2);
    }

    @Test
    public void ???????????????2() throws Exception {
        QueryResults<Member> results =
            queryFactory
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
    public void ????????????() throws Exception {

        List<Tuple> result =
            queryFactory
                .select(
                    member.count(),
                    member.age.sum(),
                    member.age.avg(),
                    member.age.max(),
                    member.age.min())
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
     * ?????? ????????? ??? ?????? ?????? ????????? ?????????
     */
    @Test
    public void group_by_??????() throws Exception {
        List<Tuple> result =
            queryFactory
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
     * ??? A??? ????????? ?????? ??????
     */
    @Test
    public void ????????????() throws Exception {

        List<Member> result =
            queryFactory
                .selectFrom(member)
                .join(team)
                .on(team.name.eq("teamA"))
                //                .where(team.name.eq("teamA"))
                .fetch();

        assertThat(result).extracting("username").containsExactly("member1", "member2");
    }

    /**
     * ????????? ????????? ???????????? ?????? ?????? ??????
     */
    @Test
    public void ??????????????????() throws Exception {

        em.persist(new Member("teamA"));
        em.persist(new Member("teamB"));
        em.persist(new Member("teamC"));

        List<Member> result =
            queryFactory.select(member).from(member, team).where(member.username.eq(team.name))
                .fetch();

        assertThat(result).extracting("username").containsExactly("teamA", "teamB");
    }

    /**
     * ????????? ?????? ???????????????, ???????????? teamA??? ?????? ??????, ????????? ?????? ?????????????? JPQL: select m, t from Member m left join
     * m.team t on t.name = 'teamA'
     */
    @Test
    public void ??????ON???_?????????() throws Exception {

        List<Tuple> result =
            queryFactory
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
     * ????????? ????????? ???????????? ?????? ????????? ?????? ??????
     */
    @Test
    public void ??????????????????????????????() throws Exception {

        em.persist(new Member("teamA"));
        em.persist(new Member("teamB"));
        em.persist(new Member("teamC"));

        List<Tuple> result =
            queryFactory
                .select(member, team)
                .from(member)
                .leftJoin(team)
                .on(member.username.eq(team.name))
                .fetch();

        for (Tuple tuple : result) {
            System.out.println("tuple = " + tuple);
        }
    }

    @Test
    public void ?????????????????????() throws Exception {

        em.flush();
        em.clear();

        System.out.println("-----------------------");

        // fetch ????????? lazy ?????? ????????? ????????? ?????????
        Member result = queryFactory.selectFrom(member).where(member.username.eq("member1"))
            .fetchOne();
        System.out.println("-----------------------");

        boolean loaded = emf.getPersistenceUnitUtil().isLoaded(result.getTeam());
        assertThat(loaded).as("?????? ?????? ?????????").isFalse();

        System.out.println("result = " + result);
    }

    @Test
    public void ??????????????????() throws Exception {

        em.flush();
        em.clear();

        System.out.println("-----------------------");

        // fetch ????????? lazy ?????? ????????? ????????? ?????????
        Member result =
            queryFactory
                .selectFrom(member)
                .join(member.team, team)
                .fetchJoin()
                .where(member.username.eq("member1"))
                .fetchOne();
        System.out.println("-----------------------");

        boolean loaded = emf.getPersistenceUnitUtil().isLoaded(result.getTeam());
        assertThat(loaded).as("?????? ?????? ??????").isTrue();

        System.out.println("result = " + result);
    }

    /**
     * ????????? ?????? ?????? ??????
     */
    @Test
    public void ????????????() {

        QMember memberSub = new QMember("memberSub");

        List<Member> result =
            queryFactory
                .selectFrom(member)
                .where(member.age.eq(select(memberSub.age.max())
                    .from(memberSub)))
                .fetch();

        assertThat(result).extracting("age").containsExactly(40);
    }

    /**
     * ????????? ?????? ????????? ?????? (goe)
     */

    @Test
    public void ????????????2() {
        QMember memberSub = new QMember("memberSub");

        List<Member> result = queryFactory.selectFrom(member)
            .where(member.age.goe(select(memberSub.age.avg())
                .from(memberSub)))
            .fetch();

        assertThat(result.size()).isEqualTo(2);
        assertThat(result.get(0).getUsername()).isEqualTo("member3");
        assertThat(result.get(1).getUsername()).isEqualTo("member4");
        assertThat(result).extracting("age").containsExactly(30, 40);

    }


    @Test
    public void ????????????3_in() {
        QMember memberSub = new QMember("memberSub");

        List<Member> result = queryFactory
            .selectFrom(member)
            .where(member.age.in(
                select(memberSub.age)
                    .from(memberSub)
                    .where(memberSub.age.gt(10))
            ))
            .fetch();

        assertThat(result).extracting("age").containsExactly(20, 30, 40);

    }

    @Test
    public void ????????????4_select() {
        QMember memberSub = new QMember("memberSub");

        List<Tuple> result = queryFactory
            .select(member.username, select(memberSub.age.avg()).from(memberSub))
            .from(member).fetch();

        for (Tuple tuple : result) {
            System.out.println("tuple = " + tuple);
        }

    }

    @Test
    public void case???() {
        List<Tuple> fetch = queryFactory
            .select(
                member.username,
                member.age
                    .when(10).then("??????")
                    .when(20).then("?????????")
                    .otherwise("??????"))
            .from(member)
            .fetch();

        for (Tuple s : fetch) {
            System.out.println("s = " + s);
        }
    }

    @Test
    public void case???_complex() {
        List<Tuple> results = queryFactory
            .select(
                member.username,
                new CaseBuilder()
                    .when(member.age.between(0, 20)).then("0~20???")
                    .when(member.age.between(21, 30)).then("21~30???")
                    .otherwise("??????"))
            .from(member).fetch();

        for (Tuple result : results) {
            System.out.println("result = " + result);
        }
    }

    @Test
    public void ????????????() {
        List<Tuple> a = queryFactory.select(member.username,
            Expressions.constant("A")

        ).from(member).fetch();

        for (Tuple tuple : a) {
            System.out.println("tuple = " + tuple);
        }
    }

    @Test
    public void ???????????????() {

        List<String> fetch = queryFactory
            .select(member.username.concat("_").concat(member.age.stringValue())).from(member)
            .where(member.username.eq("member1")).fetch();

        for (String s : fetch) {
            System.out.println("s = " + s);
        }
    }

    @Test
    public void simpleProjection() {
        List<String> fetch = queryFactory.select(member.username).from(member).fetch();

        List<Member> fetch1 = queryFactory.select(member).from(member).fetch();

        for (String s : fetch) {
            System.out.println("s = " + s);
        }

        for (Member member1 : fetch1) {
            System.out.println("member1 = " + member1);
        }

    }

    @Test
    public void tupleProjection() {

        List<Tuple> fetch = queryFactory
            .select(member.username, member.age)
            .from(member)
            .fetch();

        for (Tuple tuple : fetch) {
            String s = tuple.get(member.username);
            Integer integer = tuple.get(member.age);
        }


    }

    @Test
    public void findByJPQLDto() {
        List<MemberDto> resultList = em
            .createQuery("select new kirok.querydsl.dto.MemberDto(m.username, m.age) from Member m",
                MemberDto.class).getResultList();

        for (MemberDto memberDto : resultList) {
            System.out.println("memberDto = " + memberDto);
        }
    }

    @Test
    public void findDtoBySetter() {

        List<MemberDto> result = queryFactory
            .select(bean(MemberDto.class, member.username, member.age))
            .from(member).fetch();

        for (MemberDto memberDto : result) {
            System.out.println("memberDto = " + memberDto);
        }

    }

    @Test
    public void findDtoByField() {

        List<MemberDto> result = queryFactory
            .select(fields(MemberDto.class, member.username, member.age))
            .from(member).fetch();

        for (MemberDto memberDto : result) {
            System.out.println("memberDto = " + memberDto);
        }

    }

    @Test
    public void findDtoByConstructor() {

        List<MemberDto> result = queryFactory
            .select(constructor(MemberDto.class, member.username, member.age))
            .from(member).fetch();

        for (MemberDto memberDto : result) {
            System.out.println("memberDto = " + memberDto);
        }

    }

    @Test
    public void findUserDto() {

        List<UserDto> result = queryFactory
            .select(fields(UserDto.class, member.username.as("name"), member.age))
            .from(member).fetch();

        for (UserDto memberDto : result) {
            System.out.println("memberDto = " + memberDto);
        }

    }

    @Test
    public void findUserDtoSub() {
        QMember memberSub = new QMember("memberSub");

        List<UserDto> result = queryFactory
            .select(
                fields(
                    UserDto.class,
//                    member.username.as("name"),
                    as(member.username, "name"),
                    as(
                        JPAExpressions.select(memberSub.age.max()).from(memberSub), "age")
                ))
            .from(member).fetch();

        for (UserDto memberDto : result) {
            System.out.println("memberDto = " + memberDto);
        }

    }

    @Test
    public void findDtoByQP() {

        List<MemberDto> results = queryFactory.select(new QMemberDto(member.username, member.age))
            .from(member).fetch();

        for (MemberDto result : results) {
            System.out.println("result = " + result);
        }

    }

    @Test
    public void dynamicQuery_BooleanBuilder() {
        String usernameParam = "member1";
        Integer ageParam = null;

        List<Member> result = searchMember1(usernameParam, ageParam);
        assertThat(result.size()).isEqualTo(1);


    }

    private List<Member> searchMember1(String usernameCond, Integer ageCond) {
        BooleanBuilder builder = new BooleanBuilder();
        if (usernameCond != null) {
            builder.and(member.username.eq(usernameCond));
        }
        if (ageCond != null) {
            builder.and(member.age.eq(ageCond));
        }
        return queryFactory.selectFrom(member).where(builder).fetch();
    }


    @Test
    public void dynamicQuery_WhereParam() {
        String usernameParam = "member1";
        Integer ageParam = null;

        List<Member> result = searchMember2(usernameParam, ageParam);
        assertThat(result.size()).isEqualTo(1);
    }

    private List<Member> searchMember2(String usernameCond, Integer ageCond) {

        return queryFactory
            .selectFrom(member)
            .where(
//                usernameEq(usernameCond), ageEq(ageCond)
                allEq(usernameCond, ageCond)
            )
            .fetch();
    }

    private BooleanExpression usernameEq(String usernameCond) {
        return usernameCond != null ? member.username.eq(usernameCond) : null;
    }

    private BooleanExpression ageEq(Integer ageCond) {
        return ageCond != null ? member.age.eq(ageCond) : null;
    }

    private BooleanExpression allEq(String usernameCond, Integer ageCond) {
        return usernameEq(usernameCond).and(ageEq(ageCond));
    }

    @Test
    @Commit
    public void bulkUpdate() {
        long count = queryFactory
            .update(member)
            .set(member.username, "?????????")
            .where(member.age.lt(28))
            .execute();

        System.out.println("count = " + count);

        List<Member> resultBeforeClear = queryFactory.selectFrom(member).fetch();

        for (Member member : resultBeforeClear) {
            System.out.println("resultBeforeClear = " + member);
        }

        em.flush();
        em.clear();

        List<Member> resultAfterClear = queryFactory.selectFrom(member).fetch();

        for (Member member : resultAfterClear) {
            System.out.println("resultAfterClear = " + member);
        }


    }

    @Test
    public void bulkAdd() {

        queryFactory.update(member).set(member.age, member.age.add(1)).execute();

        queryFactory.update(member).set(member.age, member.age.subtract(1)).execute();

        queryFactory.update(member).set(member.age, member.age.multiply(2)).execute();

        queryFactory.update(member).set(member.age, member.age.divide(2)).execute();

        queryFactory.update(member).set(member.age, member.age.mod(2)).execute();

    }

    @Test
    public void bulkDelete() {

        queryFactory.delete(member).where(member.age.gt(10)).execute();

    }

    @Test
    public void sqlFunction() {

        List<Tuple> fetch = queryFactory.select(
            Expressions.stringTemplate(
                "function('replace', {0}, {1}, {2})",
                member.username,
                "member",
                "M"),
            member.username,
            member.age
        )
            .from(member)
            .fetch();

        for (Tuple s : fetch) {
            System.out.println("s = " + s);
        }
    }


    @Test
    public void sqlFunction2() {

        List<Tuple> fetch = queryFactory
            .select(
                Expressions.stringTemplate("function('upper', {0})", member.username),
                member.username,
                member.age)
            .from(member)
            .where(member.username
                .eq(Expressions.stringTemplate("function('lower', {0})", member.username)))
            .fetch();

        for (Tuple tuple : fetch) {
            System.out.println("tuple = " + tuple);
        }
    }

    @Test
    public void sqlFunction3() {

        List<Tuple> fetch = queryFactory
            .select(
                member.username.upper(),
                member.username,
                member.age)
            .from(member)
            .where(member.username
                .eq(member.username.lower()))
            .fetch();

        for (Tuple tuple : fetch) {
            System.out.println("tuple = " + tuple);
        }
    }

}
